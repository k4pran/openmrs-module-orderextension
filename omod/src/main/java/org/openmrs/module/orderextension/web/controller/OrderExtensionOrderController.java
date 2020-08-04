/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.orderextension.web.controller;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.orderextension.DrugRegimen;
import org.openmrs.module.orderextension.ExtendedOrderSet;
import org.openmrs.module.orderextension.api.OrderExtensionService;
import org.openmrs.module.orderextension.util.OrderEntryUtil;
import org.openmrs.module.orderextension.util.OrderExtensionUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Primary Controller for administering orders
 */
@Controller
public class OrderExtensionOrderController {
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());

	private OrderExtensionService getOrderExtensionService() {
		return Context.getService(OrderExtensionService.class);
	}
	
	/**
	 * Shows the page to list order sets
	 */
	@RequestMapping(value = "/module/orderextension/orderList")
	public void listOrders(ModelMap model, @RequestParam(value = "patientId", required = true) Integer patientId) {
		Patient patient = Context.getPatientService().getPatient(patientId);
		model.addAttribute("patient", patient);

		List<DrugRegimen> regimens = getOrderExtensionService().getDrugRegimens(patient);
		model.addAttribute("regimens", regimens);

		List<DrugOrder> drugOrders = OrderEntryUtil.getDrugOrdersByPatient(patient);
		for (DrugRegimen r : regimens) {
			drugOrders.removeAll(r.getOrders());
		}
		model.addAttribute("drugOrders", drugOrders);
		
		model.addAttribute("orderSets", getOrderExtensionService().getNamedOrderSets(false));
	}
	
	/**
	 * adds a new orderSet
	 */
	@RequestMapping(value = "/module/orderextension/addOrdersFromSet")
	public String addOrderSet(ModelMap model, @RequestParam(value = "patientId", required = true) Integer patientId,
	                          @RequestParam(value = "orderSet", required = true) Integer orderSetId,
	                          @RequestParam(value = "startDateSet", required = true) Date startDateSet,
	                          @RequestParam(value = "numCycles", required = false) Integer numCycles,
	                          @RequestParam(value = "returnPage", required = true) String returnPage) {
		
		Patient patient = Context.getPatientService().getPatient(patientId);
		ExtendedOrderSet orderSet = getOrderExtensionService().getOrderSet(orderSetId);
		getOrderExtensionService().addOrdersForPatient(patient, orderSet, startDateSet, numCycles);
		return "redirect:" + returnPage;
	}
	
	/**
	 * adds a new order
	 */
	@RequestMapping(value = "/module/orderextension/addDrugOrder")
	public String addOrder(ModelMap model, @RequestParam(value = "patientId", required = true) Integer patientId,
	                       @RequestParam(value = "drug", required = true) Integer drugId,
	                       @RequestParam(value = "dose", required = true) Double dose,
	                       @RequestParam(value = "doseUnits", required = true) Concept doseUnits,
	                       @RequestParam(value = "frequencyDay", required = false) String frequencyDay,
	                       @RequestParam(value = "frequencyWeek", required = false) String frequencyWeek,
	                       @RequestParam(value = "startDateDrug", required = true) Date startDateDrug,
	                       @RequestParam(value = "stopDateDrug", required = false) Date stopDateDrug,
	                       @RequestParam(value = "asNeeded", required = false) String asNeeded,
	                       @RequestParam(value = "classification", required = false) Integer classification,
	                       @RequestParam(value = "indication", required = false) Integer indication,
	                       @RequestParam(value = "instructions", required = false) String instructions,
	                       @RequestParam(value = "adminInstructions", required = false) String adminInstructions,
	                       @RequestParam(value = "returnPage", required = true) String returnPage) {
		
		DrugOrder drugOrder = setUpDrugOrder(patientId, drugId, dose, doseUnits, frequencyDay, frequencyWeek, startDateDrug,
		    stopDateDrug, asNeeded, classification, indication, instructions, adminInstructions);

		getOrderExtensionService().extendedSaveDrugOrder(drugOrder);
		
		return "redirect:" + returnPage;
	}
	
	private DrugOrder setUpDrugOrder(Integer patientId, Integer drugId, Double dose, Concept doseUnits, String frequencyDay,
	                                 String frequencyWeek, Date startDateDrug, Date stopDateDrug, String asNeeded,
	                                 Integer classification, Integer indication, String instructions,
	                                 String adminInstructions) {
		Patient patient = Context.getPatientService().getPatient(patientId);
		
		DrugOrder drugOrder = new DrugOrder();
		drugOrder.setPatient(patient);
		
		drugOrder = updateDrugOrder(drugOrder, drugId, dose, doseUnits, frequencyDay, frequencyWeek, startDateDrug, stopDateDrug,
		    asNeeded, classification, indication, instructions, adminInstructions);
		
		return drugOrder;
	}
	
	private DrugOrder updateDrugOrder(DrugOrder drugOrder, Integer drugId, Double dose, Concept doseUnits, String frequencyDay,
	                                  String frequencyWeek, Date startDateDrug, Date stopDateDrug, String asNeeded,
	                                  Integer classification, Integer indication, String instructions,
	                                  String adminInstructions) {
		Drug drug = Context.getConceptService().getDrug(drugId);
		drugOrder.setDrug(drug);
		drugOrder.setConcept(drug.getConcept());
		drugOrder.setDose(dose);
		drugOrder.setDoseUnits(doseUnits);
		
		String frequency = "";
		if (frequencyDay != null && frequencyDay.length() > 0) {
			frequency = frequencyDay;
		}
		if (frequencyWeek != null && frequencyWeek.length() > 0) {
			if (frequency.length() > 0) {
				frequency = frequency + " x ";
			}
			
			frequency = frequency + frequencyWeek;
		}
		
		OrderEntryUtil.setFrequency(drugOrder, frequency);
		OrderEntryUtil.setStartDate(drugOrder, startDateDrug);
		if (asNeeded != null) {
			drugOrder.setAsNeeded(true);
		} else {
			drugOrder.setAsNeeded(false);
		}

		if (classification != null) {
			drugOrder.setOrderReason(Context.getConceptService().getConcept(classification));
		}
		else if (indication != null) {
			drugOrder.setOrderReason(Context.getConceptService().getConcept(indication));
		}
		else {
			drugOrder.setOrderReason(null);
		}

		drugOrder.setDosingInstructions(adminInstructions);

		
		drugOrder.setInstructions(instructions);

		OrderEntryUtil.updateStopAndExpireDates(drugOrder, stopDateDrug);
		
		OrderType orderType = OrderEntryUtil.getDrugOrderType();
		drugOrder.setOrderType(orderType);
		
		return drugOrder;
	}
	
	@RequestMapping(value = "/module/orderextension/addDrugOrderToGroup")
	public String addDrugOrderToGroup(ModelMap model, @RequestParam(value = "patientId", required = true) Integer patientId,
	                                  @RequestParam(value = "groupId", required = true) Integer groupId,
	                                  @RequestParam(value = "drug", required = true) Integer drugId,
	                                  @RequestParam(value = "dose", required = true) Double dose,
									  @RequestParam(value = "doseUits", required = true) Concept doseUnits,
	                                  @RequestParam(value = "frequencyDay", required = false) String frequencyDay,
	                                  @RequestParam(value = "frequencyWeek", required = false) String frequencyWeek,
	                                  @RequestParam(value = "addCycleStartDate", required = true) Date startDateDrug,
	                                  @RequestParam(value = "stopDate", required = false) Date stopDateDrug,
	                                  @RequestParam(value = "asNeeded", required = false) String asNeeded,
	                                  @RequestParam(value = "classification", required = false) Integer classification,
	                                  @RequestParam(value = "indication", required = false) Integer indication,
	                                  @RequestParam(value = "instructions", required = false) String instructions,
	                                  @RequestParam(value = "adminInstructions", required = false) String adminInstructions,
	                                  @RequestParam(value = "repeatCycles", required = false) String repeatCycle,
	                                  @RequestParam(value = "returnPage", required = true) String returnPage) {
		
		DrugRegimen regimen = getOrderExtensionService().getDrugRegimen(groupId);
		
		if (repeatCycle != null) {
			Patient patient = Context.getPatientService().getPatient(patientId);
			List<DrugRegimen> futureOrders = getOrderExtensionService().getFutureDrugRegimensOfSameOrderSet(patient, regimen, regimen.getFirstDrugOrderStartDate());
			
			for (DrugRegimen drugRegimen : futureOrders) {
				Date startDate = adjustDate(drugRegimen.getFirstDrugOrderStartDate(), regimen.getFirstDrugOrderStartDate(),
				    startDateDrug);
				
				Date stopDate = null;
				if (stopDateDrug != null) {
					stopDate = adjustDate(drugRegimen.getFirstDrugOrderStartDate(), regimen.getFirstDrugOrderStartDate(),
					    stopDateDrug);
				}
				
				DrugOrder drugOrder = setUpDrugOrder(patientId, drugId, dose, doseUnits, frequencyDay,
				    frequencyWeek, startDate, stopDate, asNeeded, classification, indication, instructions,
				    adminInstructions);
				drugRegimen.addOrder(drugOrder);
				
				getOrderExtensionService().saveDrugRegimen(drugRegimen);
			}
		}

		DrugOrder drugOrder = setUpDrugOrder(patientId, drugId, dose, doseUnits, frequencyDay,
		    frequencyWeek, startDateDrug, stopDateDrug, asNeeded, classification, indication, instructions,
		    adminInstructions);
		regimen.addOrder(drugOrder);
		
		getOrderExtensionService().saveDrugRegimen(regimen);
		
		return "redirect:" + returnPage;
	}
	
	@RequestMapping(value = "/module/orderextension/changeStartDateOfGroup")
	public String changeStartDateOfGroup(ModelMap model,
	                                     @RequestParam(value = "patientId", required = true) Integer patientId,
	                                     @RequestParam(value = "groupId", required = true) Integer groupId,
	                                     @RequestParam(value = "changeDate", required = true) Date changeDate,
	                                     @RequestParam(value = "repeatCycles", required = false) String repeatCycle,
	                                     @RequestParam(value = "returnPage", required = true) String returnPage) {
		
		DrugRegimen regimen = getOrderExtensionService().getDrugRegimen(groupId);
		
		if (repeatCycle != null) {
			Patient patient = Context.getPatientService().getPatient(patientId);
			List<DrugRegimen> futureOrders = getOrderExtensionService()
			        .getFutureDrugRegimensOfSameOrderSet(patient, regimen, regimen.getFirstDrugOrderStartDate());
			
			for (DrugRegimen drugRegimen : futureOrders) {
				
				for (DrugOrder order : drugRegimen.getMembers()) {
					if (order.getAutoExpireDate() != null) {
						order.setAutoExpireDate(adjustDate(order.getAutoExpireDate(), regimen.getFirstDrugOrderStartDate(),
						    changeDate));
					}

					OrderEntryUtil.setStartDate(order, adjustDate(order.getEffectiveStartDate(), regimen.getFirstDrugOrderStartDate(), changeDate));
					getOrderExtensionService().extendedSaveDrugOrder(order);
				}
			}
		}
		
		Date sDate = regimen.getFirstDrugOrderStartDate();
		for (DrugOrder order : regimen.getMembers()) {
			if (order.getAutoExpireDate() != null) {
				order.setAutoExpireDate(adjustDate(order.getAutoExpireDate(), sDate, changeDate));
			}

			OrderEntryUtil.setStartDate(order, adjustDate(order.getEffectiveStartDate(), sDate, changeDate));
			getOrderExtensionService().extendedSaveDrugOrder(order);
		}
		
		return "redirect:" + returnPage;
	}
	
	@RequestMapping(value = "/module/orderextension/changeStartDateOfPartGroup")
	public String changeStartDateOfPartGroup(ModelMap model,
	                                         @RequestParam(value = "patientId", required = true) Integer patientId,
	                                         @RequestParam(value = "groupId", required = true) Integer groupId,
	                                         @RequestParam(value = "changePartDate", required = true) Date changeDate,
	                                         @RequestParam(value = "cycleDay", required = true) String cycleDayString,
	                                         @RequestParam(value = "repeatCycles", required = false) String repeatCycle,
	                                         @RequestParam(value = "repeatPartCycles", required = false) String repeatPartCycles,
	                                         @RequestParam(value = "repeatThisCycle", required = false) String repeatThisCycle,
	                                         @RequestParam(value = "returnPage", required = true) String returnPage) {
		
		DrugRegimen regimen = getOrderExtensionService().getDrugRegimen(groupId);
		
		Integer cycleDay = Integer.parseInt(cycleDayString);
		
		Date startDate = getCycleDate(regimen.getFirstDrugOrderStartDate(), cycleDay);
		
		if (repeatCycle != null || repeatPartCycles != null) {
			Patient patient = Context.getPatientService().getPatient(patientId);
			List<DrugRegimen> futureOrders = getOrderExtensionService()
			        .getFutureDrugRegimensOfSameOrderSet(patient, regimen, regimen.getFirstDrugOrderStartDate());
			
			for (DrugRegimen drugRegimen : futureOrders) {
				
				for (DrugOrder order : drugRegimen.getMembers()) {
					
					if (repeatCycle == null) {
						if (getCycleDay(drugRegimen.getFirstDrugOrderStartDate(), order.getEffectiveStartDate()) == cycleDay) {
							if (order.getAutoExpireDate() != null) {
								order.setAutoExpireDate(adjustDate(order.getAutoExpireDate(), startDate, changeDate));
							}
							
							OrderEntryUtil.setStartDate(order, adjustDate(order.getEffectiveStartDate(), startDate, changeDate));
							getOrderExtensionService().extendedSaveDrugOrder(order);
						}
					} else {
						if (order.getAutoExpireDate() != null) {
							order.setAutoExpireDate(adjustDate(order.getAutoExpireDate(), startDate, changeDate));
						}
						
						OrderEntryUtil.setStartDate(order, adjustDate(order.getEffectiveStartDate(), startDate, changeDate));
						getOrderExtensionService().extendedSaveDrugOrder(order);
					}
				}
			}
		}
		
		for (DrugOrder order : regimen.getMembers()) {
			if (repeatThisCycle != null || repeatPartCycles != null) {
				if (getCycleDay(regimen.getFirstDrugOrderStartDate(), order.getEffectiveStartDate()) >= cycleDay) {
					if (order.getAutoExpireDate() != null) {
						order.setAutoExpireDate(adjustDate(order.getAutoExpireDate(), startDate, changeDate));
					}
					
					OrderEntryUtil.setStartDate(order, adjustDate(order.getEffectiveStartDate(), startDate, changeDate));
					getOrderExtensionService().extendedSaveDrugOrder(order);
				}
			} else {
				if (getCycleDay(regimen.getFirstDrugOrderStartDate(), order.getEffectiveStartDate()) == cycleDay) {
					if (order.getAutoExpireDate() != null) {
						order.setAutoExpireDate(adjustDate(order.getAutoExpireDate(), startDate, changeDate));
					}
					
					OrderEntryUtil.setStartDate(order, adjustDate(order.getEffectiveStartDate(), startDate, changeDate));
					getOrderExtensionService().extendedSaveDrugOrder(order);
				}
			}
		}
		
		return "redirect:" + returnPage;
	}
	
	@RequestMapping(value = "/module/orderextension/editDrug")
	public String editDrug(ModelMap model, @RequestParam(value = "orderId", required = true) Integer orderId,
	                       @RequestParam(value = "drug", required = true) Integer drugId,
	                       @RequestParam(value = "dose", required = true) Double dose,
						   @RequestParam(value = "doseUnits", required = true) Concept doseUnits,
	                       @RequestParam(value = "frequencyDay", required = false) String frequencyDay,
	                       @RequestParam(value = "frequencyWeek", required = false) String frequencyWeek,
	                       @RequestParam(value = "editStartDate", required = true) Date startDateDrug,
	                       @RequestParam(value = "editStopDate", required = false) Date stopDateDrug,
	                       @RequestParam(value = "asNeeded", required = false) String asNeeded,
	                       @RequestParam(value = "classification", required = false) Integer classification,
	                       @RequestParam(value = "indication", required = false) Integer indication,
	                       @RequestParam(value = "instructions", required = false) String instructions,
	                       @RequestParam(value = "adminInstructions", required = false) String adminInstructions,
	                       @RequestParam(value = "repeatCycles", required = false) String repeatCycle,
	                       @RequestParam(value = "returnPage", required = true) String returnPage,
	                       @RequestParam(value = "drugChangeReason", required = false) Integer changeReason,
	                       @RequestParam(value = "discontinue", required = true) String discontinue,
	                       @RequestParam(value = "patientId", required = true) Integer patientId) {
		
		DrugOrder drugOrder = (DrugOrder)Context.getOrderService().getOrder(orderId);
		DrugRegimen regimen = (DrugRegimen) drugOrder.getOrderGroup();

		if (repeatCycle != null) {
			Patient patient = Context.getPatientService().getPatient(patientId);
			List<DrugOrder> futureOrders = getOrderExtensionService().getFutureDrugOrdersOfSameOrderSet(patient, regimen.getOrderSet(),
			            regimen.getFirstDrugOrderStartDate());

			for (DrugOrder order : futureOrders) {
				if ((order.getDrug() != null && drugOrder.getDrug() != null && order.getDrug().equals(
				    drugOrder.getDrug()))
				        || (order.getConcept() != null && drugOrder.getConcept() != null && order.getConcept().equals(
				            drugOrder.getConcept()))) {
					//assuming that the same drug won't appear twice in the same indication within a cycle and that you would want to change the dose on one
					if ((order.getOrderReason() == null && drugOrder.getOrderReason() == null)
					        || (order.getOrderReason() != null && drugOrder.getOrderReason() != null && drugOrder
					                .getOrderReason().equals(order.getOrderReason()))) {
						Date startDate = order.getEffectiveStartDate();
						Date endDate = order.getAutoExpireDate();
						if (drugOrder.getEffectiveStartDate().getTime() != startDateDrug.getTime()) {
							startDate = adjustDate(startDate, drugOrder.getEffectiveStartDate(), startDateDrug);
						}
						if (drugOrder.getAutoExpireDate() == null && stopDateDrug != null) {
							endDate = OrderExtensionUtil.adjustDateToEndOfDay(stopDateDrug);
						} else if (drugOrder.getAutoExpireDate() != null && stopDateDrug == null) {
							endDate = null;
						} else if (drugOrder.getAutoExpireDate() != null && stopDateDrug != null
						        && drugOrder.getAutoExpireDate().getTime() != stopDateDrug.getTime()) {
							endDate = adjustDate(endDate, drugOrder.getAutoExpireDate(), stopDateDrug);
						}

						DrugOrder orderDrug = updateDrugOrder(order, drugId, dose, doseUnits, frequencyDay, frequencyWeek,
						    startDate, endDate, asNeeded, classification, indication, instructions, adminInstructions);
						getOrderExtensionService().extendedSaveDrugOrder(orderDrug);
					}
				}
			}
		}
		
		//if there is an change reason entered, then we want to discontinue the current drug order with a reason and 
		//create a new one with the edit details.
		if(changeReason == null)
		{
			drugOrder = updateDrugOrder(drugOrder, drugId, dose, doseUnits, frequencyDay, frequencyWeek, startDateDrug, stopDateDrug, asNeeded,
					classification, indication, instructions, adminInstructions);
		}
		else
		{
			Concept stopConcept = Context.getConceptService().getConcept(changeReason);
			OrderEntryUtil.getOrderExtensionService().discontinueOrder(drugOrder, stopConcept, stopDateDrug);
			
			//if the user has edited and not chosen the discontinue button, then add a new order with the changes suggested
			if(discontinue.equals("false"))
			{
				DrugOrder newDrugOrder = setUpDrugOrder(patientId, drugId, dose, doseUnits, frequencyDay,
					    frequencyWeek, startDateDrug, stopDateDrug, asNeeded, classification, indication, instructions,
					    adminInstructions);
				
				if(regimen != null)
				{
					regimen.addOrder(newDrugOrder);
				}
			}
		}

		getOrderExtensionService().extendedSaveDrugOrder(drugOrder);
		
		return "redirect:" + returnPage;
	}
	
	@RequestMapping(value = "/module/orderextension/stopDrug")
	public String stopDrug(ModelMap model, @RequestParam(value = "orderId", required = true) Integer orderId,
	                       @RequestParam(value = "drugStopDate", required = true) Date stopDate,
	                       @RequestParam(value = "drugStopReason", required = true) Integer stopReason,
	                       @RequestParam(value = "repeatCycles", required = false) String repeatCycle,
	                       @RequestParam(value = "returnPage", required = true) String returnPage,
	                       @RequestParam(value = "patientId", required = true) Integer patientId) {
		
		Concept stopConcept = Context.getConceptService().getConcept(stopReason);
		
		DrugOrder drugOrder = (DrugOrder) Context.getOrderService().getOrder(orderId);
		if (repeatCycle != null) {
			DrugRegimen regimen = (DrugRegimen) drugOrder.getOrderGroup();
			Patient patient = Context.getPatientService().getPatient(patientId);
			List<DrugOrder> futureOrders = getOrderExtensionService()
			        .getFutureDrugOrdersOfSameOrderSet(patient, regimen.getOrderSet(),
			            regimen.getFirstDrugOrderStartDate());

			for (DrugOrder order : futureOrders) {
				if (order.getDrug() != null && drugOrder.getDrug() != null) {
					if (order.getDrug().equals(drugOrder.getDrug())) {
						Context.getOrderService().voidOrder(order, stopConcept.getDisplayString());
					}
				} else if (order.getConcept() != null && drugOrder.getConcept() != null) {
					if (order.getConcept().equals(drugOrder.getConcept())) {
						Context.getOrderService().voidOrder(order, stopConcept.getDisplayString());
					}
				}
			}
		}

		OrderEntryUtil.getOrderExtensionService().discontinueOrder(drugOrder, stopConcept, OrderExtensionUtil.adjustDateToEndOfDay(stopDate));
		
		return "redirect:" + returnPage;
	}
	
	@RequestMapping(value = "/module/orderextension/deleteDrug")
	public String deleteDrug(ModelMap model, @RequestParam(value = "orderId", required = true) Integer orderId,
	                         @RequestParam(value = "deleteReason", required = true) String voidReason,
	                         @RequestParam(value = "deleteReasonDescription", required = false) String voidReasonDescription,
	                         @RequestParam(value = "repeatCycles", required = false) String repeatCycle,
	                         @RequestParam(value = "returnPage", required = true) String returnPage,
	                         @RequestParam(value = "patientId", required = true) Integer patientId) {
		
		StringBuilder voidReasonAndDescription=new StringBuilder();
		voidReasonAndDescription.append(voidReason);
		//if(!voidReasonDescription.equals("") || voidReasonDescription!=null || voidReasonDescription.length()>0){
		if(voidReasonDescription.trim().length()>0){
			voidReasonAndDescription.append(" ");
			voidReasonAndDescription.append(voidReasonDescription);
		}
		
		Order o = Context.getOrderService().getOrder(orderId);
		DrugOrder drugOrder = (DrugOrder)o;
			
		if (repeatCycle != null) {
			DrugRegimen regimen = (DrugRegimen)drugOrder.getOrderGroup();
			Patient patient = Context.getPatientService().getPatient(patientId);
			List<DrugOrder> futureOrders = getOrderExtensionService()
			        .getFutureDrugOrdersOfSameOrderSet(patient, regimen.getOrderSet(),
			            regimen.getFirstDrugOrderStartDate());

			for (DrugOrder order : futureOrders) {
				if (order.getDrug() != null && drugOrder.getDrug() != null) {
					if (order.getDrug().equals(drugOrder.getDrug())) {
						Context.getOrderService().voidOrder(order, voidReasonAndDescription.toString());
					}
				} else if (order.getConcept() != null && drugOrder.getConcept() != null) {
					if (order.getConcept().equals(drugOrder.getConcept())) {
						Context.getOrderService().voidOrder(order, voidReasonAndDescription.toString());
					}
				}
			}
		}
		
		Context.getOrderService().voidOrder(o, voidReasonAndDescription.toString());
		
		return "redirect:" + returnPage;
	}
	
	@RequestMapping(value = "/module/orderextension/deleteAllDrugsInGroup")
	public String deleteAllDrugsInGroup(ModelMap model, @RequestParam(value = "groupId", required = true) Integer groupId,
	                                    @RequestParam(value = "deleteReason", required = true) String voidReason,
	       	                         	@RequestParam(value = "deleteAllReasonDescription", required = false) String voidReasonDescription,
	       	                         	@RequestParam(value = "repeatCycles", required = false) String repeatCycle,
	                                    @RequestParam(value = "returnPage", required = true) String returnPage,
	                                    @RequestParam(value = "patientId", required = true) Integer patientId) {
		
		
		StringBuilder voidReasonAndDescription=new StringBuilder();
		voidReasonAndDescription.append(voidReason);
		//if(!voidReasonDescription.equals("") || voidReasonDescription!=null || voidReasonDescription.length()>0){
		if(voidReasonDescription.trim().length()>0){
			voidReasonAndDescription.append(" ");
			voidReasonAndDescription.append(voidReasonDescription);
		}
		
		
		
		DrugRegimen regimen = getOrderExtensionService().getDrugRegimen(groupId);
		
		if (repeatCycle != null) {
			Patient patient = Context.getPatientService().getPatient(patientId);
			List<DrugOrder> futureOrders = getOrderExtensionService()
			        .getFutureDrugOrdersOfSameOrderSet(patient, regimen.getOrderSet(), regimen.getLastDrugOrderEndDate());
			
			for (DrugOrder order : futureOrders) {
				Context.getOrderService().voidOrder(order, voidReasonAndDescription.toString());
			}
		}
		
		for (DrugOrder order : regimen.getMembers()) {
			Context.getOrderService().voidOrder(order, voidReasonAndDescription.toString());
		}
		
		return "redirect:" + returnPage;
	}
	
	@RequestMapping(value = "/module/orderextension/stopAllDrugsInGroup")
	public String stopAllDrugsInGroup(ModelMap model, @RequestParam(value = "groupId", required = true) Integer groupId,
	                                  @RequestParam(value = "drugStopAllDate", required = true) Date stopDate,
	                                  @RequestParam(value = "drugStopAllReason", required = true) Integer stopReason,
	                                  @RequestParam(value = "repeatCycles", required = false) String repeatCycle,
	                                  @RequestParam(value = "returnPage", required = true) String returnPage,
	                                  @RequestParam(value = "patientId", required = true) Integer patientId) {
		
		DrugRegimen regimen = getOrderExtensionService().getDrugRegimen(groupId);
		
		Concept stopConcept = Context.getConceptService().getConcept(stopReason);
		
		if (repeatCycle != null) {
			Patient patient = Context.getPatientService().getPatient(patientId);
			List<DrugOrder> futureOrders = getOrderExtensionService()
			        .getFutureDrugOrdersOfSameOrderSet(patient, regimen.getOrderSet(), regimen.getLastDrugOrderEndDate());
			
			for (DrugOrder order : futureOrders) {
				Context.getOrderService().voidOrder(order, stopConcept.getDisplayString());
			}
		}
		
		for (DrugOrder order : regimen.getMembers()) {
			if (OrderEntryUtil.isCurrent(order)) {
				OrderEntryUtil.getOrderExtensionService().discontinueOrder(order, stopConcept, OrderExtensionUtil.adjustDateToEndOfDay(stopDate));
			} else if (OrderEntryUtil.isFuture(order)) {
				Context.getOrderService().voidOrder(order, stopConcept.getDisplayString());
			}
		}
		
		return "redirect:" + returnPage;
	}
	
	private Date adjustDate(Date dateToAdjust, Date startDateComparison, Date endDateComparison) {
		long milis2 = startDateComparison.getTime();
		long milis1 = endDateComparison.getTime();
		
		long diff = milis1 - milis2;
		
		long diffDays = diff / (24 * 60 * 60 * 1000);
		
		Calendar adjusted = Calendar.getInstance();
		adjusted.setTime(dateToAdjust);
		adjusted.add(Calendar.DAY_OF_YEAR, (int) diffDays);
		
		return adjusted.getTime();
	}
	
	private Integer getCycleDay(Date firstDrugStart, Date drugStart) {
		if (firstDrugStart != null && drugStart != null) {
			long cycleDay = drugStart.getTime() - firstDrugStart.getTime();
			if (cycleDay > 0) {
				cycleDay = cycleDay / 86400000;
				cycleDay = cycleDay + 1;
				return (int) cycleDay;
			}
		}
		
		return 1;
	}
	
	public Date getCycleDate(Date cycleStart, Integer day) {
		Calendar cycleDate = Calendar.getInstance();
		cycleDate.setTime(cycleStart);
		cycleDate.add(Calendar.DAY_OF_YEAR, day - 1);
		return cycleDate.getTime();
	}
}
