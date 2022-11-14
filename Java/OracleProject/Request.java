package OracleProject;
import java.math.BigInteger;
import java.util.ArrayList;

/**
 *
 * @author Amir
 */

public class Request {
    private BigInteger id;
    private BigInteger arrivalTime;
    private String sender;
    private boolean isPending;
    private String query;
    private ArrayList<String> oraclersAddress;
    private ArrayList<BigInteger> stakes;
    private Request_sol_Request request;
    private double arvTime;
    private double fshTime;
    private int requestId;
    
    public Request(
            BigInteger _id,
            BigInteger _arrivalTime,
            String _sender,
            boolean _isPending,
            String _query,
            ArrayList<String> _oraclersAddress,
            ArrayList<BigInteger> _stakes,
            Request_sol_Request _request
    ){
        setRequestId(_id);
        setRequestArrivalTime(_arrivalTime);
        setRequstSender(_sender);
        setIsPending(_isPending);
        setRequestString(_query);
        setOraclersAddress(_oraclersAddress);
        setStakes(_stakes);
        setRequestContract(_request);
    }
    
    public Request(int requestId, double arvTime, double fshTime){
        setRequestid(requestId);
        setArrivalTime(arvTime);
        setFinishTime(fshTime);
    }
    
    public void setRequestid(int requestId){
        this.requestId = requestId;
    }
    
    public int getRequestid(){
        return this.requestId;
    }
    
    public void setArrivalTime(double arvTime){
        this.arvTime = arvTime;
    }
    
    public void setFinishTime(double fshTime){
        this.fshTime = fshTime;
    }
    
    public double getArrivalTime(){
        return this.arvTime;
    }
    
    public double getFinishTime(){
        return this.fshTime;
    }
    
    public void setRequestId(BigInteger _id){
        this.id = _id;
    }
    
    public void setRequestArrivalTime(BigInteger _arrivalTime){
        this.arrivalTime = _arrivalTime;
    }
    
    public void setRequstSender(String _sender){
        this.sender = _sender;
    }
    
    public void setIsPending(boolean _isPending){
        this.isPending = _isPending;
    }
    
    public void setRequestString(String _query){
        this.query = _query;
    }
    
    public void setOraclersAddress(ArrayList<String> _oraclersAddress){
        this.oraclersAddress = _oraclersAddress;
    }
    
    public void setStakes(ArrayList<BigInteger> _stakes){
        this.stakes = _stakes;
    }
    
    public void setRequestContract(Request_sol_Request _request){
        this.request = _request;
    }
    
    public BigInteger getRequestId(){
        return this.id;
    }
    
    public BigInteger getRequestArrivalTime(){
        return this.arrivalTime;
    }
    
    public String getRequestSender(){
        return this.sender;
    }
    
    public boolean getIsPending(){
        return this.isPending;
    }
    
    public String getRequestString(){
        return this.query;
    }
    
    public ArrayList<String> getOraclersAddress(){
        return this.oraclersAddress;
    }
    
    public ArrayList<BigInteger> getStakes(){
        return this.stakes;
    }
    
    public Request_sol_Request getContractRequest(){
        return this.request;
    }
    
    @Override
    public String toString() {
        String reply = this.id + " " +
                this.arrivalTime + " " +
                this.sender + " " + 
                this.isPending + " " +
                this.query + " " +
                this.oraclersAddress + " " +
                this.stakes;
        return reply;
    }

}
