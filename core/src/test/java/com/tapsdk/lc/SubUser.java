package com.tapsdk.lc;

import com.tapsdk.lc.annotation.LCClassName;

//@JSONType(ignores = {"blackListRelation"})
@LCClassName("SubUser")
public class SubUser extends LCUser {
  public LCObject getArmor() {
    return getLCObject("armor");
  }

  public void setArmor(LCObject armor) {
    this.put("armor", armor);
  }

  public void setNickName(String name) {
    this.put("nickName", name);
  }

  public String getNickName() {
    return this.getString("nickName");
  }

  public LCRelation<LCObject> getBlackListRelation() {
    return this.getRelation("blacklist");
  }

  // public void addBlackList(AVObject o) {
  // this.getBlackListRelation().add(o);
  // this.saveInBackground();
  // }
  //
  // public void removeBlackList(AVObject o) {
  // this.getBlackListRelation().remove(o);
  // this.saveInBackground();
  // }
}