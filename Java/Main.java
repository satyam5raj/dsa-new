import java.util.Scanner;

public class Main {
    public static void main(String[] args){
        Scanner sc = new Scanner(System.in);
        System.out.println("Please enter your message: ");
        String message = sc.nextLine();
        System.out.println("Your message is: " + message);
    }
}


// This command is to save the .class to whichever dir we want
// javac -d .. Main.java    