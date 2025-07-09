package com.lawyerapp.LawyerApp.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public class Client implements Serializable {
	private static final long serialVersionUID = 1L;
	private int id;
    private int lawyerId;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String name;
    private String surname;
    private String caseNumber;
    private String court;
    private String notes;
    
    
    public Client() {
		super();
	}


    public Client(int id, int lawyerId, LocalDate appointmentDate, LocalTime appointmentTime, String name,
            String surname, String caseNumber, String court, String notes) {
        super();
        this.id = id;
        this.lawyerId = lawyerId;
        this.appointmentDate = appointmentDate;
        
        // MODIFICA: assicura che i secondi siano sempre 0
        this.appointmentTime = (appointmentTime != null) ? 
                               LocalTime.of(appointmentTime.getHour(), appointmentTime.getMinute()) : 
                               LocalTime.of(9, 0);
        
        this.name = name;
        this.surname = surname;
        this.caseNumber = caseNumber;
        this.court = court;
        this.notes = notes;
    }




	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
	}


	public int getLawyerId() {
		return lawyerId;
	}


	public void setLawyerId(int lawyerId) {
		this.lawyerId = lawyerId;
	}


	public LocalDate getAppointmentDate() {
		return appointmentDate;
	}


	public void setAppointmentDate(LocalDate appointmentDate) {
		this.appointmentDate = appointmentDate;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public String getSurname() {
		return surname;
	}


	public void setSurname(String surname) {
		this.surname = surname;
	}


	public String getCaseNumber() {
		return caseNumber;
	}


	public void setCaseNumber(String caseNumber) {
		this.caseNumber = caseNumber;
	}


	public String getCourt() {
		return court;
	}


	public void setCourt(String court) {
		this.court = court;
	}


	public String getNotes() {
		return notes;
	}


	public void setNotes(String notes) {
		this.notes = notes;
	}
	
	


	public LocalTime getAppointmentTime() {
		return appointmentTime;
	}





	public void setAppointmentTime(LocalTime appointmentTime) {
		this.appointmentTime = appointmentTime;
	}





	@Override
	public String toString() {
		return "Client [id=" + id + ", lawyerId=" + lawyerId + ", appointmentDate=" + appointmentDate
				+ ", appointmentTime=" + appointmentTime + ", name=" + name + ", surname=" + surname + ", caseNumber="
				+ caseNumber + ", court=" + court + ", notes=" + notes + "]";
	}





	@Override
	public int hashCode() {
		return Objects.hash(appointmentDate, appointmentTime, caseNumber, court, id, lawyerId, name, notes, surname);
	}





	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Client other = (Client) obj;
		return Objects.equals(appointmentDate, other.appointmentDate)
				&& Objects.equals(appointmentTime, other.appointmentTime)
				&& Objects.equals(caseNumber, other.caseNumber) && Objects.equals(court, other.court) && id == other.id
				&& lawyerId == other.lawyerId && Objects.equals(name, other.name) && Objects.equals(notes, other.notes)
				&& Objects.equals(surname, other.surname);
	}


    
}
