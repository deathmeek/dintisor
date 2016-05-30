/*
 * stimulate.h
 *
 *  Created on: Apr 18, 2016
 *      Author: dan
 */

#ifndef DINTISOR_STIMULATE_H_
#define DINTISOR_STIMULATE_H_

#include <ble.h>


void stimulate_service_init(void);
void stimulate_on_ble_event(ble_evt_t* event);
void stimulate_measurement_start(void);
void stimulate_measurement_finish(void);


#endif /* DINTISOR_STIMULATE_H_ */
