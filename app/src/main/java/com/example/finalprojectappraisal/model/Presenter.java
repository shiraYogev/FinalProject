package com.example.finalprojectappraisal.model;

import com.google.firebase.firestore.PropertyName;

public class Presenter {
    private String nameOfPresenter;   // name_of_presenter
    private String idOfPresenter;     // id_of_presenter (9 digits)
    private String typeOfPresenterId; // type_of_presenter_id
    private String roleOfPresenter;   // role_of_presenter
    private String holderStatus;      // holder_status

    public Presenter() {}

    public Presenter(String nameOfPresenter, String idOfPresenter,
                     String typeOfPresenterId, String roleOfPresenter,
                     String holderStatus) {
        this.nameOfPresenter = nameOfPresenter;
        this.idOfPresenter = idOfPresenter;
        this.typeOfPresenterId = typeOfPresenterId;
        this.roleOfPresenter = roleOfPresenter;
        this.holderStatus = holderStatus;
    }

    @PropertyName("name_of_presenter")
    public String getNameOfPresenter() { return nameOfPresenter; }
    @PropertyName("name_of_presenter")
    public void setNameOfPresenter(String v) { this.nameOfPresenter = v; }

    @PropertyName("id_of_presenter")
    public String getIdOfPresenter() { return idOfPresenter; }
    @PropertyName("id_of_presenter")
    public void setIdOfPresenter(String v) { this.idOfPresenter = v; }

    @PropertyName("type_of_presenter_id")
    public String getTypeOfPresenterId() { return typeOfPresenterId; }
    @PropertyName("type_of_presenter_id")
    public void setTypeOfPresenterId(String v) { this.typeOfPresenterId = v; }

    @PropertyName("role_of_presenter")
    public String getRoleOfPresenter() { return roleOfPresenter; }
    @PropertyName("role_of_presenter")
    public void setRoleOfPresenter(String v) { this.roleOfPresenter = v; }

    @PropertyName("holder_status")
    public String getHolderStatus() { return holderStatus; }
    @PropertyName("holder_status")
    public void setHolderStatus(String v) { this.holderStatus = v; }
}
