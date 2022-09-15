package calc.calculadora;

import java.io.Serializable;

public class PackageToServer implements Serializable {

    private int operationCode;
    private final int emisor;
    private final double num1;
    private final double num2;

    public PackageToServer(double num1, double num2, int emisor) {
        this.num1 = num1;
        this.num2 = num2;
        this.emisor = emisor;
    }

    public int getOperationCode() {
        return operationCode;
    }
    public void setOperationCode(int operationCode) {
        this.operationCode = operationCode;
    }
    public int getEmisor() {return emisor;}
    public double getNum1() {return num1;}
    public double getNum2() {return num2;}

}
