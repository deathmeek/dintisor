/*
 * battery.cpp
 *
 *  Created on: Feb 4, 2016
 *      Author: dan
 */

#include <app_error.h>
#include <ble_bas.h>

#include <nrf.h>
#include <nrf_soc.h>

#include <stdint.h>
#include <string.h>


static ble_bas_t service;		// battery service handle


/**
 * @brief Initialize BLE Battery Service.
 */
void battery_service_init(void)
{
	ble_bas_init_t settings;

	// initialize settings
	memset(&settings, 0, sizeof(settings));

	// set security levels
	BLE_GAP_CONN_SEC_MODE_SET_OPEN(&settings.battery_level_char_attr_md.cccd_write_perm);
	BLE_GAP_CONN_SEC_MODE_SET_OPEN(&settings.battery_level_char_attr_md.read_perm);
	BLE_GAP_CONN_SEC_MODE_SET_NO_ACCESS(&settings.battery_level_char_attr_md.write_perm);
	BLE_GAP_CONN_SEC_MODE_SET_OPEN(&settings.battery_level_report_read_perm);

	settings.evt_handler			= NULL;
	settings.support_notification	= true;
	settings.p_report_ref			= NULL;
	settings.initial_batt_level		= 100;

	uint32_t err_code = ble_bas_init(&service, &settings);
	APP_ERROR_CHECK(err_code);
}

/**
 * @brief Process BLE events for Battery Service.
 */
void battery_service_process_event(ble_evt_t* event)
{
	ble_bas_on_ble_evt(&service, event);
}

/**
 * @brief Configure ADC for battery measurement and start conversion.
 */
void battery_measurement_start(void)
{
	NRF_ADC->CONFIG	=	(ADC_CONFIG_RES_8bit << ADC_CONFIG_RES_Pos) |
						(ADC_CONFIG_INPSEL_SupplyOneThirdPrescaling << ADC_CONFIG_INPSEL_Pos) |
						(ADC_CONFIG_REFSEL_VBG << ADC_CONFIG_REFSEL_Pos) |
						(ADC_CONFIG_PSEL_Disabled << ADC_CONFIG_PSEL_Pos) |
						(ADC_CONFIG_EXTREFSEL_None << ADC_CONFIG_EXTREFSEL_Pos);

	// ADC needs high freq clock?
	sd_clock_hfclk_request();

	uint32_t is_running = 0;
	while(!is_running)
		sd_clock_hfclk_is_running(&is_running);

	NRF_ADC->TASKS_START = 1;
}

/**
 * @brief Get VDD voltage and update battery level.
 */
void battery_measurement_finish(void)
{
	uint8_t new_level = NRF_ADC->RESULT * 100 / 255;

	uint32_t err_code = ble_bas_battery_level_update(&service, new_level);
	if(	(err_code != NRF_SUCCESS) &&
		(err_code != NRF_ERROR_INVALID_STATE) &&
		(err_code != BLE_ERROR_NO_TX_BUFFERS) &&
		(err_code != BLE_ERROR_GATTS_SYS_ATTR_MISSING))
	{
		APP_ERROR_HANDLER(err_code);
	}

	// use the STOP task to save energy; workaround for PAN_028 rev1.5 anomaly 1?
	NRF_ADC->TASKS_STOP = 1;

	sd_clock_hfclk_release();
}
