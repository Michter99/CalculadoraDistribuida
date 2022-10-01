package calc.calculadora;

import java.io.Serializable;

public class PackageToClient implements Serializable {

    private final int emisor;
    private final double result;
    private final int operationCode;

    public PackageToClient(int emisor, double result, int operationCode) {
        this.emisor = emisor;
        this.result = result;
        this.operationCode = operationCode;
    }

    public int getEmisor() {return emisor;}
    public double getResult() {return result;}
    public int getOperationCode() {
        return operationCode;
    }
}
