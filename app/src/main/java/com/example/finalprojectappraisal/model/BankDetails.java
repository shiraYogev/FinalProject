package com.example.finalprojectappraisal.model;

import com.google.gson.annotations.SerializedName;
import com.google.firebase.firestore.PropertyName; // <-- ייבוא חשוב עבור Firebase

/**
 * The BankDetails class represents bank information extracted from bank documents.
 * This class stores all relevant bank details including branch information, loan details,
 * and property information for appraisal projects.
 */
public class BankDetails {

    @SerializedName("bank_name")
    private String bankName;              // Hebrew bank name

    @SerializedName("branch_name")
    private String branchName;            // Hebrew branch name

    @SerializedName("branch_email")
    private String branchEmail;           // Branch email address

    @SerializedName("banker_name")
    private String bankerName;            // Hebrew banker name

    @SerializedName("document_date_gre")
    private String documentDateGre;       // Date in YYYY-MM-DD format

    @SerializedName("document_date_he")
    private String documentDateHe;        // Hebrew date string

    @SerializedName("valuation_number")
    private String valuationNumber;       // Hebrew/Numbers valuation number

    @SerializedName("loan_number")
    private String loanNumber;            // Numbers only loan number

    @SerializedName("type_of_loan")
    private String typeOfLoan;            // Hebrew loan type

    @SerializedName("page1_header")
    private String page1Header;           // Hebrew page 1 header

    @SerializedName("lot_number")
    private String lotNumber;             // Numbers only lot number

    @SerializedName("main_parcel")
    private String mainParcel;            // Numbers only main parcel

    @SerializedName("sub_parcel")
    private String subParcel;             // Numbers only sub parcel

    @SerializedName("short_address")
    private String shortAddress;          // Hebrew short address

    @SerializedName("loaner_name")
    private String loanerName;            // Hebrew loaner name

    @SerializedName("loaner_id")
    private String loanerId;              // 9 digits ID

    @SerializedName("purpose_of_loan")
    private String purposeOfLoan;         // Hebrew purpose of loan

    @SerializedName("identity_of_customer")
    private String identityOfCustomer;    // Hebrew customer identity

    @SerializedName("appraisal_final_date")
    private String appraisalFinalDate;    // Date in DD/MM/YYYY format

    // Default constructor required for Firestore
    public BankDetails() {
    }

    // Full constructor
    public BankDetails(String bankName, String branchName, String branchEmail, String bankerName,
                       String documentDateGre, String documentDateHe, String valuationNumber,
                       String loanNumber, String typeOfLoan, String page1Header, String lotNumber,
                       String mainParcel, String subParcel, String shortAddress, String loanerName,
                       String loanerId, String purposeOfLoan, String identityOfCustomer,
                       String appraisalFinalDate) {
        this.bankName = bankName;
        this.branchName = branchName;
        this.branchEmail = branchEmail;
        this.bankerName = bankerName;
        this.documentDateGre = documentDateGre;
        this.documentDateHe = documentDateHe;
        this.valuationNumber = valuationNumber;
        this.loanNumber = loanNumber;
        this.typeOfLoan = typeOfLoan;
        this.page1Header = page1Header;
        this.lotNumber = lotNumber;
        this.mainParcel = mainParcel;
        this.subParcel = subParcel;
        this.shortAddress = shortAddress;
        this.loanerName = loanerName;
        this.loanerId = loanerId;
        this.purposeOfLoan = purposeOfLoan;
        this.identityOfCustomer = identityOfCustomer;
        this.appraisalFinalDate = appraisalFinalDate;
    }

    // Getters and setters - ***כולל תיקון ה-@PropertyName***

    @PropertyName("bank_name")
    public String getBankName() {
        return bankName;
    }

    @PropertyName("bank_name")
    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    @PropertyName("branch_name")
    public String getBranchName() {
        return branchName;
    }

    @PropertyName("branch_name")
    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    @PropertyName("branch_email")
    public String getBranchEmail() {
        return branchEmail;
    }

    @PropertyName("branch_email")
    public void setBranchEmail(String branchEmail) {
        this.branchEmail = branchEmail;
    }

    @PropertyName("banker_name")
    public String getBankerName() {
        return bankerName;
    }

    @PropertyName("banker_name")
    public void setBankerName(String bankerName) {
        this.bankerName = bankerName;
    }

    @PropertyName("document_date_gre")
    public String getDocumentDateGre() {
        return documentDateGre;
    }

    @PropertyName("document_date_gre")
    public void setDocumentDateGre(String documentDateGre) {
        this.documentDateGre = documentDateGre;
    }

    @PropertyName("document_date_he")
    public String getDocumentDateHe() {
        return documentDateHe;
    }

    @PropertyName("document_date_he")
    public void setDocumentDateHe(String documentDateHe) {
        this.documentDateHe = documentDateHe;
    }

    @PropertyName("valuation_number")
    public String getValuationNumber() {
        return valuationNumber;
    }

    @PropertyName("valuation_number")
    public void setValuationNumber(String valuationNumber) {
        this.valuationNumber = valuationNumber;
    }

    @PropertyName("loan_number")
    public String getLoanNumber() {
        return loanNumber;
    }

    @PropertyName("loan_number")
    public void setLoanNumber(String loanNumber) {
        this.loanNumber = loanNumber;
    }

    @PropertyName("type_of_loan")
    public String getTypeOfLoan() {
        return typeOfLoan;
    }

    @PropertyName("type_of_loan")
    public void setTypeOfLoan(String typeOfLoan) {
        this.typeOfLoan = typeOfLoan;
    }

    @PropertyName("page1_header")
    public String getPage1Header() {
        return page1Header;
    }

    @PropertyName("page1_header")
    public void setPage1Header(String page1Header) {
        this.page1Header = page1Header;
    }

    @PropertyName("lot_number")
    public String getLotNumber() {
        return lotNumber;
    }

    @PropertyName("lot_number")
    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    @PropertyName("main_parcel")
    public String getMainParcel() {
        return mainParcel;
    }

    @PropertyName("main_parcel")
    public void setMainParcel(String mainParcel) {
        this.mainParcel = mainParcel;
    }

    @PropertyName("sub_parcel")
    public String getSubParcel() {
        return subParcel;
    }

    @PropertyName("sub_parcel")
    public void setSubParcel(String subParcel) {
        this.subParcel = subParcel;
    }

    @PropertyName("short_address")
    public String getShortAddress() {
        return shortAddress;
    }

    @PropertyName("short_address")
    public void setShortAddress(String shortAddress) {
        this.shortAddress = shortAddress;
    }

    @PropertyName("loaner_name")
    public String getLoanerName() {
        return loanerName;
    }

    @PropertyName("loaner_name")
    public void setLoanerName(String loanerName) {
        this.loanerName = loanerName;
    }

    @PropertyName("loaner_id")
    public String getLoanerId() {
        return loanerId;
    }

    @PropertyName("loaner_id")
    public void setLoanerId(String loanerId) {
        this.loanerId = loanerId;
    }

    @PropertyName("purpose_of_loan")
    public String getPurposeOfLoan() {
        return purposeOfLoan;
    }

    @PropertyName("purpose_of_loan")
    public void setPurposeOfLoan(String purposeOfLoan) {
        this.purposeOfLoan = purposeOfLoan;
    }

    @PropertyName("identity_of_customer")
    public String getIdentityOfCustomer() {
        return identityOfCustomer;
    }

    @PropertyName("identity_of_customer")
    public void setIdentityOfCustomer(String identityOfCustomer) {
        this.identityOfCustomer = identityOfCustomer;
    }

    @PropertyName("appraisal_final_date")
    public String getAppraisalFinalDate() {
        return appraisalFinalDate;
    }

    @PropertyName("appraisal_final_date")
    public void setAppraisalFinalDate(String appraisalFinalDate) {
        this.appraisalFinalDate = appraisalFinalDate;
    }

    @Override
    public String toString() {
        return "BankDetails{" +
                "bankName='" + bankName + '\'' +
                ", branchName='" + branchName + '\'' +
                ", branchEmail='" + branchEmail + '\'' +
                ", bankerName='" + bankerName + '\'' +
                ", documentDateGre='" + documentDateGre + '\'' +
                ", documentDateHe='" + documentDateHe + '\'' +
                ", valuationNumber='" + valuationNumber + '\'' +
                ", loanNumber='" + loanNumber + '\'' +
                ", typeOfLoan='" + typeOfLoan + '\'' +
                ", page1Header='" + page1Header + '\'' +
                ", lotNumber='" + lotNumber + '\'' +
                ", mainParcel='" + mainParcel + '\'' +
                ", subParcel='" + subParcel + '\'' +
                ", shortAddress='" + shortAddress + '\'' +
                ", loanerName='" + loanerName + '\'' +
                ", loanerId='" + loanerId + '\'' +
                ", purposeOfLoan='" + purposeOfLoan + '\'' +
                ", identityOfCustomer='" + identityOfCustomer + '\'' +
                ", appraisalFinalDate='" + appraisalFinalDate + '\'' +
                '}';
    }
}