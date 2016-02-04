/*
 * battery.h
 *
 *  Created on: Feb 4, 2016
 *      Author: dand
 */

#ifndef DINTISOR_BATTERY_H_
#define DINTISOR_BATTERY_H_

#include <ble_bas.h>


#define BATTERY_LEVEL_MEAS_INTERVAL		APP_TIMER_TICKS(2000, APP_TIMER_PRESCALER) // battery level measurement interval (ticks)


void battery_service_init(void);
void battery_service_process_event(ble_evt_t* event);
void battery_measurement_start(void);
void battery_measurement_finish(void);


#endif /* DINTISOR_BATTERY_H_ */
