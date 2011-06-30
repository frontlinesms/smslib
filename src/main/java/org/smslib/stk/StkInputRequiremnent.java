package org.smslib.stk;

public class StkInputRequiremnent extends StkResponse {
 private StkMenu stkMenu;//stkMenu from which the inputRequirement is generated from


 public StkInputRequiremnent(StkMenu stkMenu) {
  super();
  this.stkMenu = stkMenu;
 }

 // Generation of an StkMenuItem
 public StkRequest getRequest() {
  if (stkMenu.getMenuItems().size()==1){
    return stkMenu.getMenuItems().get(0);
  } else {
   return StkMenuItem.ERROR;
  }
 }
 
 public StkRequest getRequest(String menuOption) throws StkMenuItemNotFoundException {
  for(StkMenuItem m : this.stkMenu.getMenuItems()) {
   if(m.getText().equals(menuOption)) {
    return m;
   }
  }
  throw new StkMenuItemNotFoundException();
 }
}