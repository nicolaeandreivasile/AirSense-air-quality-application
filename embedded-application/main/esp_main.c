#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/random.h>

#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_system.h"
#include "esp_log.h"
#include "nvs_flash.h"
#include "esp_bt.h"
#include "esp_bt_main.h"
#include "esp_bt_defs.h"
#include "esp_gatts_api.h"
#include "esp_gap_ble_api.h"

#include "esp_main.h"

static const char *TAG = "esp_main";

static void nvs_partition_cleanup(void);
static void bt_controller_cleanup(void);
static void bluedroid_stack_cleanup(void);

/*
 * Bluetooth LE
 */

/* GATT attribute variables */
static uint16_t attr_handle_table[IDX_NO];

/* Primary service UUID */
static uint8_t env_sensing_service_uuid128[16] = {
	/* LSB <--------> MSB */
	0xfb, 0x34, 0x9b, 0x5f, 0x80, 0x00,
	0x00, 0x80, 
	0x00, 0x10, 
	0x00, 0x00,
	0x1a, 0x18, 0x00, 0x00,
};

/* 
 * Control register for advertise and scan response data 
 *
 * Bit   | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
 *       +---+---+---+---+---+---+---+---+
 * Value | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
 *
 * Bit 0 - set HIGH for advertising data
 * Bit 1 - set HIGH for scan respose data
 */
static uint8_t esp_gap_ble_data_control_reg = 0;

/* Advertise GAP data */
static esp_ble_adv_data_t adv_data = {
	.set_scan_rsp = false,
	.include_name = true,
	.include_txpower = true,
	.min_interval = 0x0006,
	.max_interval = 0x0010,
	.appearance = 0x00,
	.manufacturer_len = 0,
	.p_manufacturer_data =  NULL,
	.service_data_len = 0,
	.p_service_data = NULL,
	.service_uuid_len = sizeof(env_sensing_service_uuid128),
	.p_service_uuid = env_sensing_service_uuid128,
	.flag = (ESP_BLE_ADV_FLAG_GEN_DISC | ESP_BLE_ADV_FLAG_BREDR_NOT_SPT),
};

#ifdef ESP_ENV_APP_SCAN_RSP
/* Scan response GAP data */
static esp_ble_adv_data_t rsp_data = {
	.set_scan_rsp = true,
	.include_name = true,
	.include_txpower = true,
	.min_interval = 0x0006,
	.max_interval = 0x0010,
	.appearance = 0x00,
	.manufacturer_len = 0,
	.p_manufacturer_data =  NULL,
	.service_data_len = 0,
	.p_service_data = NULL,
	.service_uuid_len = sizeof(env_sensing_service_uuid128),
	.p_service_uuid = env_sensing_service_uuid128,
	.flag = (ESP_BLE_ADV_FLAG_GEN_DISC | ESP_BLE_ADV_FLAG_BREDR_NOT_SPT),
};
#endif

/* Advertise GAP parameters */
static esp_ble_adv_params_t adv_params = {
    .adv_int_min        = 0x20,
    .adv_int_max        = 0x40,
    .adv_type           = ADV_TYPE_IND,
    .own_addr_type      = BLE_ADDR_TYPE_PUBLIC,
    //.peer_addr        =
    //.peer_addr_type   =
    .channel_map        = ADV_CHNL_ALL,
    .adv_filter_policy  = ADV_FILTER_ALLOW_SCAN_ANY_CON_ANY,
};


/*
 * GATTS Attribute Database
 */
/* Primary service parameters */
static uint16_t primary_service_uuid =		ESP_GATT_UUID_PRI_SERVICE;
static uint16_t env_sensing_service_uuid16 = 	ESP_GATT_UUID_ENVIRONMENTAL_SENSING_SVC;

static uint16_t char_declaration_uuid16 = 	ESP_GATT_UUID_CHAR_DECLARE;

/* Control */
static uint16_t char_control_uuid16 =		0x2A9F;
static uint8_t char_control_prop = 		ESP_GATT_CHAR_PROP_BIT_WRITE;
static uint8_t char_control_value =		0;

/* Temperature */
static uint16_t char_temperature_uuid16 = 	0x2A6E;
static uint8_t char_temperature_prop = 		ESP_GATT_CHAR_PROP_BIT_WRITE | ESP_GATT_CHAR_PROP_BIT_READ;
static uint8_t char_temperature_value = 	26;

/* Humidity */
static uint16_t char_humidity_uuid16 = 		0x2A6F;
static uint8_t char_humidity_prop = 		ESP_GATT_CHAR_PROP_BIT_WRITE | ESP_GATT_CHAR_PROP_BIT_READ;
static uint8_t char_humidity_value = 		50;

/* Pressure */
static uint16_t char_pressure_uuid16 = 		0x2A6D;
static uint8_t char_pressure_prop = 		ESP_GATT_CHAR_PROP_BIT_WRITE | ESP_GATT_CHAR_PROP_BIT_READ;
static uint8_t char_pressure_value = 		1;

/* Gas */
static uint16_t char_gas_uuid16 = 		0x2ACA;
static uint8_t char_gas_prop = 			ESP_GATT_CHAR_PROP_BIT_WRITE | ESP_GATT_CHAR_PROP_BIT_READ;
static uint8_t char_gas_value = 		110;

/* Light */
static uint16_t char_light_uuid16 =		0x2B03;
static uint8_t char_light_prop = 		ESP_GATT_CHAR_PROP_BIT_WRITE | ESP_GATT_CHAR_PROP_BIT_READ;
static uint8_t char_light_value = 		43;

/* GATT Server Database */
static const esp_gatts_attr_db_t gatt_database[IDX_NO] = {
	/* Primary service declaration */
	[PRI_SERVICE_IDX] = {
		{ESP_GATT_AUTO_RSP},
		{ESP_UUID_LEN_16, (uint8_t *) &primary_service_uuid,
			ESP_GATT_PERM_READ, sizeof(uint16_t), 
			sizeof(env_sensing_service_uuid16), 
			(uint8_t *) &env_sensing_service_uuid16},
	},
	
	/* Control characteristic declaration */
	[CHAR_DECLARATION_CONTROL_IDX] = {
		{ESP_GATT_AUTO_RSP},
		{ESP_UUID_LEN_16, (uint8_t *) &char_declaration_uuid16,
			ESP_GATT_PERM_READ, sizeof(uint8_t), 
			sizeof(char_control_prop), 
			(uint8_t *) &char_control_prop},
	},
	
	/* Control characteristic value */
	[CHAR_VALUE_CONTROL_IDX] = {
		{ESP_GATT_AUTO_RSP},
		{ESP_UUID_LEN_16, (uint8_t *) &char_control_uuid16,
			ESP_GATT_PERM_WRITE, 
			sizeof(uint8_t), sizeof(char_control_value), 
			(uint8_t *) &char_control_value},
	},

	/* Temperature characteristic declaration */
	[CHAR_DECLARATION_TEMPERATURE_IDX] = {
		{ESP_GATT_AUTO_RSP},
		{ESP_UUID_LEN_16, (uint8_t *) &char_declaration_uuid16,
			ESP_GATT_PERM_READ, sizeof(uint8_t), 
			sizeof(char_temperature_prop), 
			(uint8_t *) &char_temperature_prop},
	},
	
	/* Temperature characteristic value */
	[CHAR_VALUE_TEMPERATURE_IDX] = {
		{ESP_GATT_AUTO_RSP},
		{ESP_UUID_LEN_16, (uint8_t *) &char_temperature_uuid16,
			ESP_GATT_PERM_READ | ESP_GATT_PERM_WRITE, 
			sizeof(uint8_t), sizeof(char_temperature_value), 
			(uint8_t *) &char_temperature_value},
	},
	
	/* Humidity characteristic declaration */
	[CHAR_DECLARATION_HUMIDITY_IDX] = {
		{ESP_GATT_AUTO_RSP},
		{ESP_UUID_LEN_16, (uint8_t *) &char_declaration_uuid16,
			ESP_GATT_PERM_READ, sizeof(uint8_t), 
			sizeof(char_humidity_prop), 
			(uint8_t *) &char_humidity_prop},
	},
	
	/* Humidity characteristic value */
	[CHAR_VALUE_HUMIDITY_IDX] = {
		{ESP_GATT_AUTO_RSP},
		{ESP_UUID_LEN_16, (uint8_t *) &char_humidity_uuid16,
			ESP_GATT_PERM_READ | ESP_GATT_PERM_WRITE, 
			sizeof(uint8_t), sizeof(char_humidity_value), 
			(uint8_t *) &char_humidity_value},
	},
	
	/* Pressure characteristic declaration */
	[CHAR_DECLARATION_PRESSURE_IDX] = {
		{ESP_GATT_AUTO_RSP},
		{ESP_UUID_LEN_16, (uint8_t *) &char_declaration_uuid16,
			ESP_GATT_PERM_READ, sizeof(uint8_t), 
			sizeof(char_pressure_prop), 
			(uint8_t *) &char_pressure_prop},
	},
	
	/* Pressure characteristic value */
	[CHAR_VALUE_PRESSURE_IDX] = {
		{ESP_GATT_AUTO_RSP},
		{ESP_UUID_LEN_16, (uint8_t *) &char_pressure_uuid16,
			ESP_GATT_PERM_READ | ESP_GATT_PERM_WRITE, 
			sizeof(uint8_t), sizeof(char_pressure_value), 
			(uint8_t *) &char_pressure_value},
	},

	/* Gas characteristic declaration */
	[CHAR_DECLARATION_GAS_IDX] = {
		{ESP_GATT_AUTO_RSP},
		{ESP_UUID_LEN_16, (uint8_t *) &char_declaration_uuid16,
			ESP_GATT_PERM_READ, sizeof(uint8_t), 
			sizeof(char_gas_prop), 
			(uint8_t *) &char_gas_prop},
	},
	
	/* Gas characteristic value */
	[CHAR_VALUE_GAS_IDX] = {
		{ESP_GATT_AUTO_RSP},
		{ESP_UUID_LEN_16, (uint8_t *) &char_gas_uuid16,
			ESP_GATT_PERM_READ | ESP_GATT_PERM_WRITE, 
			sizeof(uint8_t), sizeof(char_gas_value), 
			(uint8_t *) &char_gas_value},
	},

	/* Light characteristic declaration */
	[CHAR_DECLARATION_LIGHT_IDX] = {
		{ESP_GATT_AUTO_RSP},
		{ESP_UUID_LEN_16, (uint8_t *) &char_declaration_uuid16,
			ESP_GATT_PERM_READ, sizeof(uint8_t), 
			sizeof(char_light_prop), 
			(uint8_t *) &char_light_prop},
	},
	
	/* Light characteristic value */
	[CHAR_VALUE_LIGHT_IDX] = {
		{ESP_GATT_AUTO_RSP},
		{ESP_UUID_LEN_16, (uint8_t *) &char_light_uuid16,
			ESP_GATT_PERM_READ | ESP_GATT_PERM_WRITE, 
			sizeof(uint8_t), sizeof(char_light_value), 
			(uint8_t *) &char_light_value},
	},
};


/* GATTS profile A event handler definition */
static void gatts_profile_a_event_handler(esp_gatts_cb_event_t event,
		esp_gatt_if_t gatts_if, esp_ble_gatts_cb_param_t *param);

/* Profile instances */
static struct gatt_profile_instance profile_instances[ESP_ENV_APP_PROFILE_NO] = {
	[ESP_ENV_APP_PROFILE_A_ID] = {
		.gatts_cb = gatts_profile_a_event_handler,
		.gatts_if = ESP_GATT_IF_NONE,
	},
};

void esp_gatt_update_characteristics(esp_ble_gatts_cb_param_t *param) {

	if (param->read.handle == 
			attr_handle_table[CHAR_VALUE_GAS_IDX]) {
		getrandom(&char_gas_value, sizeof(uint8_t), 0);
		char_gas_value = (char_gas_value % 3) + 110;
		esp_ble_gatts_set_attr_value(param->read.handle, 
				(uint16_t) sizeof(char_gas_value), 
				&char_gas_value);
	} else if (param->read.handle == 
			attr_handle_table[CHAR_VALUE_LIGHT_IDX]) {
		getrandom(&char_light_value, sizeof(uint8_t), 0);
		char_light_value = (char_light_value % 3) + 43;	
		esp_ble_gatts_set_attr_value(param->read.handle, 
				(uint16_t) sizeof(char_light_value), 
				&char_light_value);
	} else if (param->read.handle == 
			attr_handle_table[CHAR_VALUE_HUMIDITY_IDX]) {
		getrandom(&char_humidity_value, sizeof(uint8_t), 0);
		char_humidity_value = (char_humidity_value % 3) + 49;	
		esp_ble_gatts_set_attr_value(param->read.handle, 
				(uint16_t) sizeof(char_humidity_value), 
				&char_humidity_value);
	}
}

/* GATTS profile A event handler body*/
static void gatts_profile_a_event_handler(esp_gatts_cb_event_t event,
		esp_gatt_if_t gatts_if, esp_ble_gatts_cb_param_t *param)
{
	esp_err_t err;

	switch(event) {
	case ESP_GATTS_REG_EVT:
		ESP_LOGI(TAG, "**** GATTS profile A event: "
				"ESP_GATTS_REG_EVT ****");
		ESP_LOGI(TAG, "Register params. status: %d, app_id: %x",
				param->reg.status, param->reg.app_id);
		
		err = esp_ble_gap_set_device_name(ESP_DEVICE_NAME);
		if (err) {
			ESP_LOGE(TAG, "[%s: %d]: Device name configuration "
					"failed. Error code: %x",
					__func__, __LINE__, err);
			break;
		}
		
		err = esp_ble_gap_config_adv_data(&adv_data);
		if (err) {
			ESP_LOGE(TAG, "[%s: %d]: Adv data configuration "
					"failed. Error code: %x", __func__,
					__LINE__, err);
			break;
		}
		esp_gap_ble_data_control_reg |= SET_ADV_DATA_BIT_REG;

#ifdef ESP_ENV_APP_SCAN_RSP
		err = esp_ble_gap_config_adv_data(&rsp_data);
		if (err) {
			ESP_LOGE(TAG, "[%s: %d]: Scan rsp data configuration "
					"failed. Error code: %x", 
					__func__, __LINE__, err);
			break;
		}
		esp_gap_ble_data_control_reg |= SET_SCAN_RSP_DATA_BIT_REG;
#endif

		err = esp_ble_gatts_create_attr_tab(gatt_database, gatts_if,
				IDX_NO, ESP_ENV_APP_SERVICE_ID);
		if (err) {
			ESP_LOGE(TAG, "[%s: %d]: Attribute table creation "
					"failed. Error code: %x", __func__,
					__LINE__, err);
			break;
		}
		break;
	case ESP_GATTS_READ_EVT:
		ESP_LOGI(TAG, "**** GATTS profile A event: "
				"ESP_GATTS_READ_EVT ****");
		ESP_LOGI(TAG, "Read params. conn_id: %d, trans_id: %d, "
				"handle: %x, need_rsp: %d, is_long: %d, "
				"offset: %d, bda:",
				param->read.conn_id, param->read.trans_id, 
				param->read.handle, param->read.need_rsp,
				param->read.is_long, param->read.offset);
		ESP_LOG_BUFFER_HEX_LEVEL(TAG, param->read.bda,
				ESP_BD_ADDR_LEN, ESP_LOG_INFO);

		esp_gatt_update_characteristics(param); 	
		break;
	case ESP_GATTS_WRITE_EVT:
		ESP_LOGI(TAG, "**** GATTS profile A event: "
				"ESP_GATTS_WRITE_EVT ****");
		ESP_LOGI(TAG, "Write params. conn_id: %d, trans_id: %d, "
				"handle: %x, need_rsp: %d, is_prep: %d, bda:",
				param->write.conn_id, param->write.trans_id, 
				param->write.handle, param->write.need_rsp,
				param->write.is_prep);
		ESP_LOG_BUFFER_HEX_LEVEL(TAG, param->write.bda,
				ESP_BD_ADDR_LEN, ESP_LOG_INFO);

		memcpy(&char_control_value, param->write.value, sizeof(uint8_t));
		break;
	case ESP_GATTS_EXEC_WRITE_EVT:
		ESP_LOGI(TAG, "**** GATTS profile A event: "
				"ESP_GATTS_EXEC_WRITE_EVT ****");
		ESP_LOGI(TAG, "Exec write params. conn_id: %d, trans_id: %d, "
				"exec_write_flag: %d, bda:",
				param->exec_write.conn_id, 
				param->exec_write.trans_id, 
				param->exec_write.exec_write_flag);
		ESP_LOG_BUFFER_HEX_LEVEL(TAG, param->exec_write.bda,
				ESP_BD_ADDR_LEN, ESP_LOG_INFO);
		break;
	case ESP_GATTS_MTU_EVT:
		ESP_LOGI(TAG, "**** GATTS profile A event: "
				"ESP_GATTS_MTU_EVT ****");
		ESP_LOGI(TAG, "MTU params. conn_id: %d, mtu: %d",
				param->mtu.conn_id, param->mtu.mtu);
		break;
	case ESP_GATTS_CONF_EVT:
		ESP_LOGI(TAG, "**** GATTS profile A event: "
				"ESP_GATTS_CONF_EVT ****");
		ESP_LOGI(TAG, "Confirm params. conn_id: %d, status: %x, "
				"handle: %d, len: %d, value:",
				param->conf.conn_id, param->conf.status,
				param->conf.handle, param->conf.len);
		ESP_LOG_BUFFER_HEX_LEVEL(TAG, param->conf.value,
				param->conf.len, ESP_LOG_INFO);
		break;
	case ESP_GATTS_CONNECT_EVT:
		ESP_LOGI(TAG, "**** GATTS profile A event: "
				"ESP_GATTS_CONNECT_EVT ****");
		ESP_LOGI(TAG, "Connect params. conn_id: %d, link_role: %d, "
				"remote_bda:",
				param->connect.conn_id,
				param->connect.link_role);
		ESP_LOG_BUFFER_HEX_LEVEL(TAG, param->connect.remote_bda,
				ESP_BD_ADDR_LEN, ESP_LOG_INFO);
		break;
	case ESP_GATTS_DISCONNECT_EVT:
		ESP_LOGI(TAG, "**** GATTS profile A event: "
				"ESP_GATTS_DISCONNECT_EVT ****");
		ESP_LOGI(TAG, "Disconnect params. conn_id: %d, reason: %x, "
				"remote_bda:",
				param->disconnect.conn_id,
				param->disconnect.reason);
		ESP_LOG_BUFFER_HEX_LEVEL(TAG, param->disconnect.remote_bda,
				ESP_BD_ADDR_LEN, ESP_LOG_INFO);

		switch (char_control_value) {
		case CONTROL_REBOOT:
			esp_restart();
			break;
		default:
			break;
		}
		
		err = esp_ble_gap_start_advertising(&adv_params);
		if (err) {
			ESP_LOGE(TAG, "[%s: %d]: BLE start advertising on "
				       "disconnect failed. Error code: %x",
				       __func__, __LINE__, err);	
			break;
		}
		break;
	case ESP_GATTS_CREAT_ATTR_TAB_EVT:
		ESP_LOGI(TAG, "**** GATTS profile A event: "
				"ESP_GATTS_CREAT_ATTR_TAB_EVT ****");
		if (param->add_attr_tab.status != ESP_GATT_OK) {
			ESP_LOGE(TAG, "[%s: %d]: Attribute table creation "
					"failed. Errror code: %x", __func__,
					__LINE__, param->add_attr_tab.status);
	 	} else if (param->add_attr_tab.num_handle != IDX_NO) {
			ESP_LOGE(TAG, "[%s: %d]: Attribute table created "
					"abnormally. num_handle (%d) != "
					"IDX_NO(%d)", __func__, __LINE__, 
					param->add_attr_tab.num_handle,
					IDX_NO);
		} else {
			memcpy(attr_handle_table, param->add_attr_tab.handles,
					sizeof(attr_handle_table));
			err = esp_ble_gatts_start_service(
					attr_handle_table[PRI_SERVICE_IDX]);
			if (err) {
				ESP_LOGE(TAG, "[%s: %d]: Starting service "
						"failed. Error code: %x",
						__func__, __LINE__, err);
				break;
			}
		}
		break;
	default:
		break;	
	}
	
}

/* GATTS event handler */
static void gatts_event_handler(esp_gatts_cb_event_t event,
		esp_gatt_if_t gatts_if, esp_ble_gatts_cb_param_t *param)
{
	int index;
	
	/* If event is register event, store the gatts_if for each profile */
	if (event == ESP_GATTS_REG_EVT) {
		if (param->reg.status == ESP_GATT_OK) {
			profile_instances[param->reg.app_id].gatts_if =
				gatts_if;
		} else {
			ESP_LOGI(TAG, "Registration app failed, "
					"app_id: %x, status: %d\n", 
					param->reg.app_id, param->reg.status);
			return;
        	}
    	}

	/* Call each profile's callback */
    	do {
        	for (index = 0; index < ESP_ENV_APP_PROFILE_NO; index++) {
            		if (gatts_if == ESP_GATT_IF_NONE || gatts_if ==
					profile_instances[index].gatts_if) {
                		if (profile_instances[index].gatts_cb)
                    			profile_instances[index].gatts_cb(
						event, gatts_if, param);
            		}
		}
	} while (0);
}

/* GAP event handler */
static void gap_event_handler(esp_gap_ble_cb_event_t event,
		esp_ble_gap_cb_param_t *param)
{
	esp_err_t err;

	switch(event) {
	case ESP_GAP_BLE_ADV_DATA_SET_COMPLETE_EVT:
		ESP_LOGI(TAG, "**** GAP event: "
				"ESP_GAP_BLE_ADV_DATA_SET_COMPLE_EVT ****");
		esp_gap_ble_data_control_reg &= (~SET_ADV_DATA_BIT_REG);
		if (esp_gap_ble_data_control_reg == 0) {
			err = esp_ble_gap_start_advertising(&adv_params);
			if (err) {
				ESP_LOGE(TAG, "[%s: %d]: BLE start "
						"advertising failed. Error "
						"code: %x", __func__,
						__LINE__, err);	
				break;
			}
		}
		break;
#ifdef ESP_ENV_APP_SCAN_RSP
	case ESP_GAP_BLE_SCAN_RSP_DATA_SET_COMPLETE_EVT:	
		ESP_LOGI(TAG, "**** GAP event: "
				"ESP_GAP_BLE_SCAN_RSP_DATA_SET_COMPLETE_EVT "
				"****");
		esp_gap_ble_data_control_reg &= (~SET_SCAN_RSP_DATA_BIT_REG);
		if (esp_gap_ble_data_control_reg == 0) {
			err = esp_ble_gap_start_advertising(&adv_params);
			if (err) {
				ESP_LOGE(TAG, "[%s: %d]: BLE start "
						"advertising failed. Error "
						"code: %x", __func__,
						__LINE__, err);	
				break;
			}
		}
		break;
#endif
	case ESP_GAP_BLE_ADV_START_COMPLETE_EVT:
		ESP_LOGI(TAG, "**** GAP event: "
				"ESP_GAP_BLE_ADV_START_COMPLETE_EVT ****");
		if (param->adv_start_cmpl.status != ESP_BT_STATUS_SUCCESS)
			ESP_LOGE(TAG, "---- Advertising start failed ----");
		else
			ESP_LOGI(TAG, "---- Advertising start "
					"successfully ----");
		break;
	case ESP_GAP_BLE_AUTH_CMPL_EVT:
		ESP_LOGI(TAG, "**** GAP event: "
				"ESP_GAP_BLE_AUTH_CMPL_EVT ****");
		break;
	default:
		break;
	}
}


/* 
 * NVS partition related functions
 */

/* NVS partition initialization */
static esp_err_t nvs_partition_init(void)
{
	esp_err_t err;

	ESP_LOGI(TAG, "Initializing NVS partition...");
	/* NVS flash init */
	err = nvs_flash_init();
	if (err == ESP_ERR_NVS_NO_FREE_PAGES) {
		ESP_LOGE(TAG, "[%s: %d]: NVS partition initialization failed. "
				"No free pages left. Error: %s", 
				__func__, __LINE__, esp_err_to_name(err));
		ESP_LOGI(TAG, "Trying to cleanup NVS partition...");
		err = nvs_flash_erase();
		if (err) {
			ESP_LOGE(TAG, "[%s]: NVS partition cleanup failed. "
					"Error: %s",
					__func__, esp_err_to_name(err));
			return err;
		}
		ESP_LOGI(TAG, "Trying to initialize NVS partition "
				"once again...");
		err = nvs_flash_init();
	}
	if (err) {
		ESP_LOGE(TAG, "[%s: %d]: NVS partition initialization failed. "
				"Error: %s", __func__, __LINE__,
				esp_err_to_name(err));
		return err;
	}
	ESP_LOGI(TAG, "Initializing NVS partition done");
	
	return ESP_OK;
}

/* NVS partition cleanup */
static void nvs_partition_cleanup(void)
{
	esp_err_t err;
	
	ESP_LOGI(TAG, "Cleaning up NVS partition");
	/* Erasing NVS partition */
	err = nvs_flash_erase();
	if (err) {
		ESP_LOGE(TAG, "[%s: %d]: NVS partition cleanup failed. "
				"Error: %s", __func__, __LINE__,
				esp_err_to_name(err));
		return;
	}
}


/* 
 * BT controller related functions
 * init / cleanup
 */
 
/* Bluetooth controller initialization */
static esp_err_t bt_controller_init(void)
{
	esp_err_t err;

	ESP_LOGI(TAG, "Initializing BT controller...");	
	/* BT controller initialization with default configuration */	
	esp_bt_controller_config_t bt_config = 
		BT_CONTROLLER_INIT_CONFIG_DEFAULT();
	err = esp_bt_controller_init(&bt_config);
	if (err) {
		ESP_LOGE(TAG, "[%s: %d]: BT controller initialization failed. "
				"Error: %s", __func__, __LINE__,
				esp_err_to_name(err));
		return err;
	}
	ESP_LOGI(TAG, "Initializing BT controller done");
	
	ESP_LOGI(TAG, "Enabling BT controller...");
	/* BT controller enable in BLE mode */
	err = esp_bt_controller_enable(ESP_BT_MODE_BLE);
	if (err) {
		ESP_LOGE(TAG, "[%s: %d]: BT controller enabling failed. "
				"Error: %s", __func__, __LINE__,
				esp_err_to_name(err));
		goto bt_controller_deinit;
	}
	ESP_LOGI(TAG, "Enabling BT controller done");

	return ESP_OK;

bt_controller_deinit:
	ESP_LOGI(TAG, "Cleanup BT controller initialization");
	err = esp_bt_controller_deinit();
	if (err)
		ESP_LOGE(TAG, "[%s: %d] BT controller deinitialization "
				"failed. Error: %s", __func__, __LINE__,
				esp_err_to_name(err));
	return err;
}

/* Bluetooth controller cleanup */
static void bt_controller_cleanup(void)
{
	esp_err_t err;
	
	ESP_LOGI(TAG, "Cleaning up BT controller");
	/* Disabling BT controller */
	err = esp_bt_controller_disable();
	if (err) {
		ESP_LOGE(TAG, "[%s: %d]: BT controller disabling failed. "
				"Error: %s", __func__, __LINE__,
				esp_err_to_name(err));
		return;
	}
	
	/* Deinitializing BT controller */
	err = esp_bt_controller_deinit();
	if (err) {
		ESP_LOGE(TAG, "[%s: %d]: BT controller deinitialization "
				"failed. Error: %s", __func__, __LINE__,
				esp_err_to_name(err));
		return;
	}
}


/* 
 * Bluedroid stack related functions
 * init / cleanup
 */
 
/* Bluedroid stack initialization */
static esp_err_t bluedroid_stack_init(void)
{
	esp_err_t err;
	
	ESP_LOGI(TAG, "Initializing Bluedroid stack...");
	/* Bluedroid initialization */
	err = esp_bluedroid_init();
	if (err) {
		ESP_LOGE(TAG, "[%s: %d]: Bluedroid initialization failed. "
				"Error: %s", __func__, __LINE__, 
				esp_err_to_name(err));
		return err;
	}
	ESP_LOGI(TAG, "Initializing Bluedroid stack done");

	ESP_LOGI(TAG, "Enabling Bluedroid stack...");
	/* Bluedroid enable */
	err = esp_bluedroid_enable();
	if (err) {
		ESP_LOGE(TAG, "[%s: %d]: Bluedoid enabling failed. Error: %s",
				__func__, __LINE__, esp_err_to_name(err));
		goto bluedroid_stack_deinit;
	}
	ESP_LOGI(TAG, "Enabling Bluedroid stack done");

	return ESP_OK;

bluedroid_stack_deinit:
	ESP_LOGI(TAG, "Cleaning up Bluedroid stack initialization");
	err = esp_bluedroid_deinit();
	if (err)
		ESP_LOGE(TAG, "[%s: %d]: Bluedroid deinitialization failed "
				"Error: %s", __func__, __LINE__,
				esp_err_to_name(err));
	return err;
}

/* Bluedroid stack cleanup */
static void bluedroid_stack_cleanup(void)
{
	esp_err_t err;
	
	ESP_LOGI(TAG, "Cleaning up Bluedroid stack");
	/* Disabling bluedroid stack */
	err = esp_bluedroid_disable();
	if (err) {
		ESP_LOGE(TAG, "[%s: %d]: Bluedroid disabling failed. "
				"Error: %s", __func__, __LINE__,
				esp_err_to_name(err));
		return;
	}
	
	/* Deinitializaing Bluedroid stack */
	err = esp_bluedroid_deinit();
	if (err) {
		ESP_LOGE(TAG, "[%s: %d]: Bluedroid deinitialization failed. "
				"Error: %s", __func__, __LINE__,
				esp_err_to_name(err));
		return;
	}
}


/*
 * Bluetooth LE registering
 */
static esp_err_t bluetooth_le_register(void)
{
	esp_err_t err;

	ESP_LOGI(TAG, "Registering GATTS server callback...");
	/* Register GATT callback */
	err = esp_ble_gatts_register_callback(gatts_event_handler);
	if (err) {
		ESP_LOGE(TAG, "[%s: %d]: GATTS callback registration failed. "
				"Error code: %x", __func__, __LINE__, err);
		return err;
	}
	ESP_LOGI(TAG, "Registering GATTS server callback done");
	
	ESP_LOGI(TAG, "Registering GAP server callback...");
	/* Register GAP callback */
	err = esp_ble_gap_register_callback(gap_event_handler);
	if (err) {
		ESP_LOGE(TAG, "[%s: %d]: GAP callback registration failed. "
				"Error code: %x", __func__, __LINE__, err);
		return err;
	}
	ESP_LOGI(TAG, "Registering GAP server callback done");
	
	ESP_LOGI(TAG, "Registering profile A...");
	/* Register application profile */
	err = esp_ble_gatts_app_register(ESP_ENV_APP_PROFILE_A_ID);
	if (err) {
		ESP_LOGE(TAG, "[%s: %d]: GATTS application registration "
				"failed. Error code = %x", __func__, 
				__LINE__, err);
		return err;
	}
	ESP_LOGI(TAG, "Registering profile A done");

	return ESP_OK;
}


/* 
 * Main function 
 */
void app_main(void)
{
	esp_err_t err;

	err = nvs_partition_init();
	if (err)
		return;
	err = bt_controller_init();
	if (err) {
		nvs_partition_cleanup();
		return;
	}
	err = bluedroid_stack_init();
	if (err) {
		bt_controller_cleanup();
		nvs_partition_cleanup();
		return;
	}

	err = bluetooth_le_register();
	if (err) {
		bluedroid_stack_cleanup();
		bt_controller_cleanup();
		nvs_partition_cleanup();
		return;
	}
}
