#ifndef ESP_MAIN_H__
#define ESP_MAIN_H__	1

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <esp_bt_defs.h>


/* I2C */
#define ESP_I2C_MASTER_TX_BUF_DIS 0
#define ESP_I2C_MASTER_RX_BUF_DIS 0


/* Bluetooth LE */
#define ESP_DEVICE_NAME				"Airsense ES"

#define ESP_ENV_APP_PROFILE_NO			1
#define ESP_ENV_APP_PROFILE_A_ID		0
#define ESP_ENV_APP_SERVICE_ID			0

/* GAP data MACROS for the control register */
//#define ESP_ENV_APP_SCAN_RSP			1
#define SET_ADV_DATA_BIT_REG			(1 << 0)
#ifdef ESP_ENV_APP_SCAN_RSP
#define SET_SCAN_RSP_DATA_BIT_REG		(1 << 1)
#endif

#define ESP_ENV_APP_ATTR_BUFFER_MAXSIZE	1024

enum {
	PRI_SERVICE_IDX,
	
	CHAR_DECLARATION_CONTROL_IDX,
	CHAR_VALUE_CONTROL_IDX,

	CHAR_DECLARATION_TEMPERATURE_IDX,
	CHAR_VALUE_TEMPERATURE_IDX,

	CHAR_DECLARATION_HUMIDITY_IDX,
	CHAR_VALUE_HUMIDITY_IDX,
	
	CHAR_DECLARATION_PRESSURE_IDX,
	CHAR_VALUE_PRESSURE_IDX,
	
	CHAR_DECLARATION_GAS_IDX,
	CHAR_VALUE_GAS_IDX,

	CHAR_DECLARATION_LIGHT_IDX,
	CHAR_VALUE_LIGHT_IDX,

	IDX_NO,
};

enum {
	CONTROL_NONE,

	CONTROL_REBOOT,
};

struct gatt_profile_instance {
	esp_gatts_cb_t gatts_cb;
	esp_gatt_if_t gatts_if;
};
#endif
