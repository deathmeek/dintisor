/*
 * sense.h
 *
 *  Created on: Feb 8, 2016
 *      Author: dan
 */

#ifndef DINTISOR_SENSE_H_
#define DINTISOR_SENSE_H_

#include <ble.h>
#include <ble_types.h>


ble_uuid_t service_uuid;


void sense_service_init(void);
void sense_on_ble_event(ble_evt_t* event);
void sense_measurement_start(void);
void sense_measurement_finish(void);


#endif /* DINTISOR_SENSE_H_ */
