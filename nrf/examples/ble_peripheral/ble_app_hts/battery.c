/*
 * battery.cpp
 *
 *  Created on: Feb 4, 2016
 *      Author: dan
 */

#include <app_error.h>
#include <ble_bas.h>

#include <nrf.h>
#ifdef NRF51
#include <nrf_drv_adc.h>
#endif /* NRF51 */
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
 * @brief Configure hardware for battery measurement.
 */
void battery_measurement_init(void)
{
#ifdef NRF51
	static nrf_drv_adc_channel_t channel = {
			.config.config = {
				.resolution = NRF_ADC_CONFIG_RES_8BIT,
				.input = NRF_ADC_CONFIG_SCALING_SUPPLY_ONE_THIRD,
				.reference = NRF_ADC_CONFIG_REF_VBG,
				.ain = NRF_ADC_CONFIG_INPUT_DISABLED,
			},
	};
	nrf_drv_adc_channel_enable(&channel);
#endif /* NRF51 */
}

/**
 * @brief Get VDD voltage and update battery level.
 */
void battery_measurement_sample(int16_t sample)
{
	uint8_t new_level = sample * 100 / 255;

	uint32_t err_code = ble_bas_battery_level_update(&service, new_level);
	if(	(err_code != NRF_SUCCESS) &&
		(err_code != NRF_ERROR_INVALID_STATE) &&
		(err_code != BLE_ERROR_NO_TX_PACKETS) &&
		(err_code != BLE_ERROR_GATTS_SYS_ATTR_MISSING))
	{
		APP_ERROR_HANDLER(err_code);
	}
}
