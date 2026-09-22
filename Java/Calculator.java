import java.util.Scanner;

public class Calculator{
    public static void main(String[] args){
        Scanner input = new Scanner(System.in);
        int ans = 0;
        while(true){
            System.out.print("Enter the operator : ");
            char op = input.next().trim().charAt(0);

            if(op =='+' || op == '-' || op == '*' || op == '/'){
                System.out.print("Enter two numbers : ");
                int n1 = input.nextInt();
                int n2 = input.nextInt();

                if(op == '+'){
                    ans = n1 + n2;
                }
                if(op == '-'){
                    ans =  n1 - n2;
                }
                if(op == '*'){
                    ans = n1 * n2;
                }
                if(op == '/'){
                    ans = n1 / n2;
                }
            }else if(op == 'x' || op == 'X'){
                break;
            }else{
                System.out.println("Invalid operation!!");
            }
            System.out.println("The output is : " + ans);
        }
    }
}