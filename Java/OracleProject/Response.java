package OracleProject;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Amir
 */
public class Response {
    
    private BigInteger responseId;
    private BigInteger requestId;
    private ArrayList<BigInteger> arrivalTimes;
    private ArrayList<byte[]> responses;
    private ArrayList<String> oraclerAddress;
    private boolean hasOutcome;
    private Response_sol_Response responseContract;
    
    public Response(
            BigInteger _responseId,
            BigInteger _requestId,
            ArrayList<BigInteger> _arrivalTimes,
            ArrayList<byte[]> _responses,
            ArrayList<String> _oraclerAddress,
            boolean _hasOutcome,
            Response_sol_Response _responseContract
    ){
        setResponseId(_responseId);
        setRequestId(_requestId);
        setArrivalTimes(_arrivalTimes);
        setResponses(_responses);
        setOraclerAddress(_oraclerAddress);
        setHasOutcome(_hasOutcome);
        setResponseContract(_responseContract);
    }
    
    public void setResponseId(BigInteger _responseId){
        this.responseId = _responseId;
    }
    
    public void setRequestId(BigInteger _requestId){
        this.requestId = _requestId;
    }
    
    public void setArrivalTimes(ArrayList<BigInteger> _arrivalTimes){
        this.arrivalTimes = _arrivalTimes;
    }
    
    public void setResponses(ArrayList<byte[]> _responses){
        this.responses = _responses;
    }
    
    public void setOraclerAddress(ArrayList<String> _oraclerAddress){
        this.oraclerAddress = _oraclerAddress;
    }
    
    public void setHasOutcome(boolean _hasOutcome){
        this.hasOutcome = _hasOutcome;
    }
    
    public void setResponseContract(Response_sol_Response _response){
        this.responseContract = _response;
    }
    
    public BigInteger getResponseId(){
        return this.responseId;
    }
    
    public BigInteger getRequestId(){
        return this.requestId;
    }
    
    public ArrayList<BigInteger> getArrivalTimes(){
        return this.arrivalTimes;
    }
    
    public ArrayList<byte[]> getResponse(){
        return this.responses;
    }
    
    public ArrayList<String> getOraclerAddress(){
        return this.oraclerAddress;
    }
    
    public boolean getHasOutcome(){
        return this.hasOutcome;
    }
    
    public Response_sol_Response getResponseContract(){
        return this.responseContract;
    }
    
    private List<String> convertToString(){
        List<String> convertedStr = new ArrayList<>();
        for(byte[] b: this.responses){
            convertedStr.add(new String(b));
        }
        return convertedStr;
    }
    
    @Override
    public String toString() {
        String reply = this.responseId + " " +
                this.requestId + " " +
                this.arrivalTimes + " " + 
                this.responses + " " + 
                convertToString() + " " +
                this.oraclerAddress + " " +
                this.hasOutcome;
        return reply;
    }
}
