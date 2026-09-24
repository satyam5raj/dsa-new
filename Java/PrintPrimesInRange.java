import java.util.Scanner;

public class PrintPrimesInRange{
    public static void main(String[] args){
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter start of range : ");
        int start = sc.nextInt();
        System.out.println("Enter end of range : ");
        int end = sc.nextInt();

        for(int i=start; i<=end; i++){
            if(isPrime(i)){
                System.out.println(i);
            }
        }
    }

    static boolean isPrime(int n){
        if(n <= 1){
            return false;
        }
        int c = 2;
        while(c*c <= n){
            if(n % c == 0){
                return false;
            }
            c++;
        }
        return true;
    }
}