package com.secureexam.desktop;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing a student in the SecureExam system.
 * Contains all profile information for a student.
 */
public class Student {
    private String uid;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String regNumber;
    private String stream;
    private String branch;
    private String course;
    private String className;
    private String section;
    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String dateOfBirth;
    private String gender;
    private String profilePictureUrl;
    private String bio;
    private Map<String, Object> additionalAttributes;

    /**
     * Default constructor
     */
    public Student() {
        this.additionalAttributes = new HashMap<>();
    }

    /**
     * Constructor with essential fields
     */
    public Student(String uid, String email, String firstName, String lastName, String regNumber) {
        this();
        this.uid = uid;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.regNumber = regNumber;
    }

    /**
     * Create a Student object from a map of attributes (used when loading from Firestore)
     */
    public static Student fromMap(String uid, Map<String, Object> data) {
        Student student = new Student();
        student.setUid(uid);
        student.setEmail((String) data.get("email"));
        student.setFirstName((String) data.get("firstName"));
        student.setLastName((String) data.get("lastName"));
        student.setPhone((String) data.get("phone"));
        student.setRegNumber((String) data.get("reg_number"));
        student.setStream((String) data.get("stream"));
        student.setBranch((String) data.get("branch"));
        student.setCourse((String) data.get("course"));
        student.setClassName((String) data.get("class"));
        student.setSection((String) data.get("section"));
        student.setAddress((String) data.get("address"));
        student.setCity((String) data.get("city"));
        student.setState((String) data.get("state"));
        student.setCountry((String) data.get("country"));
        student.setPostalCode((String) data.get("postalCode"));
        student.setDateOfBirth((String) data.get("dateOfBirth"));
        student.setGender((String) data.get("gender"));
        student.setProfilePictureUrl((String) data.get("profilePictureUrl"));
        student.setBio((String) data.get("bio"));
        
        // Store any additional fields that might be in the data but not explicitly modeled
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!student.hasExplicitField(entry.getKey())) {
                student.additionalAttributes.put(entry.getKey(), entry.getValue());
            }
        }
        
        return student;
    }

    /**
     * Convert the Student object to a map for storing in Firestore
     */
    public Map<String, Object> toMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("firstName", firstName);
        data.put("lastName", lastName);
        data.put("phone", phone);
        data.put("reg_number", regNumber);
        data.put("stream", stream);
        data.put("branch", branch);
        data.put("course", course);
        data.put("class", className);
        data.put("section", section);
        data.put("address", address);
        data.put("city", city);
        data.put("state", state);
        data.put("country", country);
        data.put("postalCode", postalCode);
        data.put("dateOfBirth", dateOfBirth);
        data.put("gender", gender);
        data.put("profilePictureUrl", profilePictureUrl);
        data.put("bio", bio);
        data.put("role", "student"); // Ensure role is always set
        
        // Add any additional attributes
        data.putAll(additionalAttributes);
        
        return data;
    }

    /**
     * Check if a field is explicitly modeled in this class
     */
    private boolean hasExplicitField(String fieldName) {
        return fieldName.equals("email") || 
               fieldName.equals("firstName") || 
               fieldName.equals("lastName") || 
               fieldName.equals("phone") || 
               fieldName.equals("reg_number") || 
               fieldName.equals("stream") || 
               fieldName.equals("branch") || 
               fieldName.equals("course") || 
               fieldName.equals("class") || 
               fieldName.equals("section") || 
               fieldName.equals("address") || 
               fieldName.equals("city") || 
               fieldName.equals("state") || 
               fieldName.equals("country") || 
               fieldName.equals("postalCode") || 
               fieldName.equals("dateOfBirth") || 
               fieldName.equals("gender") || 
               fieldName.equals("profilePictureUrl") || 
               fieldName.equals("bio") || 
               fieldName.equals("role");
    }

    /**
     * Get the full name of the student
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else {
            return email; // Fallback to email if name is not available
        }
    }

    // Getters and Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRegNumber() {
        return regNumber;
    }

    public void setRegNumber(String regNumber) {
        this.regNumber = regNumber;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Object getAdditionalAttribute(String key) {
        return additionalAttributes.get(key);
    }

    public void setAdditionalAttribute(String key, Object value) {
        additionalAttributes.put(key, value);
    }
}