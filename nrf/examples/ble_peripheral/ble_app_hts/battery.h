/*
 * battery.h
 *
 *  Created on: Feb 4, 2016
 *      Author: dand
 */

#ifndef DINTISOR_BATTERY_H_
#define DINTISOR_BATTERY_H_

#include <ble_bas.h>

#include <stdint.h>


#define BATTERY_LEVEL_MEAS_INTERVAL		APP_TIMER_TICKS(2000, APP_TIMER_PRESCALER) // battery level measurement interval (ticks)


void battery_service_init(void);
void battery_service_process_event(ble_evt_t* event);

uint8_t battery_measurement_init(uint8_t adc_channel);
void battery_measurement_sample(int16_t sample);


#endif /* DINTISOR_BATTERY_H_ */
