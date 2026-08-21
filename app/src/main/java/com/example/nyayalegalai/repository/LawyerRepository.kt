package com.example.nyayalegalai.repository

import com.example.nyayalegalai.models.Lawyer

class LawyerRepository {
    fun getLawyers(): List<Lawyer> {
        return listOf(
            Lawyer(1, "Advocate Rajesh Kumar", "Criminal", "15 years", "Chennai", 4.8, "+91 98765 43210", "rajesh.kumar@law.com"),
            Lawyer(2, "Advocate Priya Sharma", "Family", "10 years", "Delhi", 4.7, "+91 87654 32109", "priya.sharma@law.com"),
            Lawyer(3, "Advocate Arun Kumar", "Property", "12 years", "Bangalore", 4.6, "+91 76543 21098", "arun.kumar@law.com"),
            Lawyer(4, "Advocate Sneha Patel", "Cyber", "8 years", "Mumbai", 4.9, "+91 65432 10987", "sneha.patel@law.com"),
            Lawyer(5, "Advocate Vivek Singh", "Labour", "14 years", "Hyderabad", 4.5, "+91 54321 09876", "vivek.singh@law.com")
        )
    }
}
