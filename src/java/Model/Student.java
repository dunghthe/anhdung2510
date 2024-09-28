/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model;

import java.util.Scanner;

/**
 *
 * @author TRONG TAI
 */
public class Student {
    String name;
    int age;

    public Student() {
    }

    public Student(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public String toString() {
        return "Student{" + "name=" + name + ", age=" + age + '}';
    }
    
    void display(){
        System.out.println("Name: " + name);
        System.out.println("Age: " + age);
    }
    void getInformation(){
        Scanner sc = new Scanner(System.in);
        System.out.print("Please enter your name:" );
        name = sc.nextLine();
        System.out.print("Please enter your age:");
        age= sc.nextInt();
    }
    public static void main(String args[]){
       
        Student s2 = new Student();
        
        s2.getInformation();
        s2.display();
    }
}
