package calc.calculadora;

import java.io.Serializable;

public class PackageToClient implements Serializable {

    private final int emisor;
    private final double result;

    public PackageToClient(int emisor, double result) {
        this.emisor = emisor;
        this.result = result;
    }

    public int getEmisor() {return emisor;}
    public double getResult() {return result;}
    
}
