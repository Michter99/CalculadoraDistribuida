package calc.calculadora;

import java.io.Serializable;

public class Package implements Serializable {

    private int operationCode;
    private int emisor;
    private double num1;
    private double num2;
    private double result;
    private char lastTypeOfEmisor;
    private char packageType;
    private String event;
    private String footprint;
    private boolean recognizedOp;
    private boolean proccesedByServer;
    private int originalEmisor;

    public Package(char packageType, int emisor) {
        this.packageType = packageType;
        this.emisor = emisor;
    }

    public void setPackageType(char packageType) { this.packageType = packageType; }
    public char getPackageType() { return packageType; }

    public void setOperationCode(int operationCode) { this.operationCode = operationCode; }
    public int getOperationCode() { return operationCode; }

    public void setEmisor(int emisor) { this.emisor = emisor; }
    public int getEmisor() {return emisor;}

    public void setLastTypeOfEmisor(char lastTypeOfEmisor) { this.lastTypeOfEmisor = lastTypeOfEmisor; }
    public char getLastTypeOfEmisor() { return lastTypeOfEmisor; }

    public void setNum1(double num1) { this.num1 = num1; }
    public double getNum1() {return num1;}

    public void setNum2(double num2) { this.num2 = num2; }
    public double getNum2() {return num2;}

    public void setResult(double result) { this.result = result; }
    public double getResult() {return result; }

    public void setEvent(String event) {this.event = event; }
    public String getEvent() {return event; }

    public void setFootprint(String footprint) { this.footprint = footprint; }
    public String getFootprint() { return footprint; }

    public void setRecognizedOp(boolean recognizedOp) {this.recognizedOp = recognizedOp; }
    public boolean isRecognizedOp() {return recognizedOp; }

    public void setProccesedByServer(boolean proccesedByServer) { this.proccesedByServer = proccesedByServer; }
    public boolean isProccesedByServer() { return proccesedByServer; }

    public void setOriginalEmisor(int originalEmisor) { this.originalEmisor = originalEmisor; }
    public int getOriginalEmisor() { return originalEmisor; }
}
