import java.util.Scanner;

public class Fibo{
    public static void main(String[] args){
        Scanner input = new Scanner(System.in);
        System.out.print("Enter the nth number of series to know the number: ");
        int num = input.nextInt();

        int a = 0;
        int b = 1;
        int count = 2;

        if (num < 0) {
            System.out.println("Please enter a non-negative number.");
            return;
        }

        if (num == 0) {
            System.out.println("The 0th number is: 0");
            return;
        }

        if (num == 1) {
            System.out.println("The 1st number is: 1");
            return;
        }

        while(count <= num){
            int temp = b;
            b = b + a;
            a = temp;
            count++;
        }

        System.out.print("The " + num + "th number is : " + b);
    }
}



// import java.util.Scanner;

// public class Fibo {

//     static int fibonacci(int n) {

//         if (n == 0) {
//             return 0;
//         }

//         if (n == 1) {
//             return 1;
//         }

//         return fibonacci(n - 1) + fibonacci(n - 2);
//     }

//     public static void main(String[] args) {

//         Scanner input = new Scanner(System.in);

//         System.out.print("Enter the nth number: ");
//         int num = input.nextInt();

//         System.out.println(
//             "The " + num + "th Fibonacci number is: " + fibonacci(num)
//         );

//         input.close();
//     }
// }