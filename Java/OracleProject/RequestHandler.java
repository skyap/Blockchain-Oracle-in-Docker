package OracleProject;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.web3j.abi.EventEncoder;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.utils.Convert;
import java.math.BigInteger;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Transfer;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;

/**
 *
 * @author Amir
 */
public class RequestHandler {

    private Request_sol_Request requestContract;
    private Response_sol_Response responseContract;
    private List<Request_sol_Request.OraclersToRequestAssignedEventResponse> buffer;
    private int bufferCapacity = 10;
    private EthFilter requestAssignedFilter;
    private EthFilter requestTransferBond;
    private int timeWindow;
//    private final static String INTERPREDATA = "https://tracebaseapi.azurewebsites.net/BeefValidation/";
    private final static String INTERPREDATA = "https://tracebaseapi.azurewebsites.net/AgliveBeef/RFID/";
    private final String EMPTY = "0x0000000000000000000000000000000000000000";
    private List<AccountContainer> accountContainers;
    private Web3j web3j;
//    private String gasConsumption = "gas.txt";

    public RequestHandler(
            Request_sol_Request _request,
            Response_sol_Response _response,
            List<AccountContainer> _accountContainers,
            Web3j _web3j
    ) {
        this.buffer = new ArrayList<>();
        this.accountContainers = new ArrayList<>();
        this.web3j = _web3j;
        this.requestAssignedFilter = createContractFilter(
                _request.getContractAddress(),
                EventEncoder.encode(Request_sol_Request.ORACLERSTOREQUESTASSIGNED_EVENT)
        );
//        this.requestTransferBond = createContractFilter(
//                _request.getContractAddress(),
//                EventEncoder.encode(Request_sol_Request.TRANSFERORACLERBOND_EVENT)
//        );
        setRequestContract(_request);
        setResponseContract(_response);
        this.accountContainers = _accountContainers;
        try {
            this.timeWindow = _response.
                    getResponseTimeWindow().
                    send().intValue();
        } catch (Exception ex) {
            Logger.getLogger(RequestHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void writeGasConsumptionIntoTheFile(String fileName, String gas){
        FileOutputStream gasWriter;
        DataOutputStream dos;
        try {
            gasWriter = new FileOutputStream(fileName, true);
            dos = new DataOutputStream(gasWriter);
            dos.writeBytes(gas);
            dos.writeBytes(",\n");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(RequestHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(RequestHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    private static Credentials getPrivateKeyFromAddress(String address) {
        return Credentials.create(address);
    }

    public String fetchPrivateKeyByAddress(String address) {
        String prvKey = null;
        for (AccountContainer ac : accountContainers) {
            if (ac.getAccountAddress().equals(address)) {
                prvKey = ac.getAccountPrivateKey();
                break;
            }
        }
        return prvKey;
    }

    private static String getUrlContent(String url, String id) throws IOException {
        String content = null;
        String apiUrl = INTERPREDATA + id;
        URL _url = new URL(apiUrl);
        URLConnection urlConnection = _url.openConnection();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8));
            content = reader.lines().collect(Collectors.joining("\n"));
        } catch (MalformedURLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        return content;
    }

    public void setRequestContract(Request_sol_Request _request) {
        this.requestContract = _request;
    }

    public void setResponseContract(Response_sol_Response _response) {
        this.responseContract = _response;
    }

    private EthFilter createContractFilter(String contractAddress, String topic) {
        EthFilter filter = new EthFilter(
                DefaultBlockParameterName.LATEST,
                DefaultBlockParameterName.LATEST,
                contractAddress
        );
        filter.addSingleTopic(topic);
        return filter;
    }

    private byte[] stringToByte(String input) {
        byte[] byteValue = new byte[32];
        System.arraycopy(input.getBytes(), 0, byteValue, 0, input.getBytes().length);
        return byteValue;
    }

    private String randomStringGenerator(int length) {
        final String chars = "0123456789abcdefghigklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVXYZ@#$%&*!";
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder stringBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            stringBuilder.append(
                    chars.charAt(secureRandom.nextInt(chars.length()))
            );
        }
        return stringBuilder.toString();
    }

    private void fetchRequestData(
            BigInteger _requestId,
            Object[] _candidateOraclers,
            Response_sol_Response _deployedResponseContract,
            Request_sol_Request _deployedRequestContract
    ) throws Exception {
        double duration = 0;
        double currentTime = System.currentTimeMillis();
        String fileName = "gas_" + _candidateOraclers.length +".txt";
        System.out.println("Candidate oraclers = " + _candidateOraclers.length);
        for (int i = 0; (i < _candidateOraclers.length); i++) {
            String _oracler = _candidateOraclers[i].toString();
//            byte[] response = stringToByte(randomStringGenerator(32));
            byte[] request = _deployedRequestContract.
                    getRequestQuery(_requestId).send();
            String content = getUrlContent(INTERPREDATA, new String(request));
            duration = System.currentTimeMillis();
            double diff = duration - currentTime;
            System.out.println(new String(request) + " >>>>>>>>>>>>> "
                    + content + " "
                    + currentTime + " "
                    + duration + " "
                    + diff + " "
                    + diff / 1000
            );
            if (diff / 1000 > timeWindow) {
                break;
            }

            byte[] response = stringToByte(content);
            try {
                _deployedResponseContract.createResponse(
                        _oracler,
                        response,
                        _requestId
                ).send();
            } catch (Exception ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        BigInteger responseId = _deployedResponseContract.whatIsRequestResponseId(_requestId).send();
        BigInteger leastArrivalTime = _deployedResponseContract.getResponseLeastArrivalTime(responseId).send();
        List<BigInteger> arrivalTimes = _deployedResponseContract.getResponsesArrivalTimes(responseId).send();
        for (BigInteger bint : arrivalTimes) {
            System.out.println(" >> " + bint.intValue() + " " + leastArrivalTime.intValue());// +" " + threads.size());
        }
        System.out.println("Finalizing response for id : " + responseId.intValue() + " ");// + leastArrivalTime.intValue());
        TransactionReceipt receipt = _deployedResponseContract.processResponse(responseId).send();//Async(); working
        System.out.println(receipt.getCumulativeGasUsed() + " G>>A>>S " + receipt.getGasUsed());
        writeGasConsumptionIntoTheFile(fileName, receipt.getCumulativeGasUsed().toString());
    }

    public void produce() throws InterruptedException {
        this.requestContract.oraclersToRequestAssignedEventFlowable(requestAssignedFilter).subscribe(
                reply -> {
                    System.out.println(
                            "Handling the request id "
                            + reply.requestId.intValue()
                    );
                    this.buffer.add(reply);

                }
        );
        while (true) {
            synchronized (this) {
                while (this.buffer.size() == this.bufferCapacity) {
                    wait();
                }
                notify();

            }
        }
    }

    public void consume() throws InterruptedException, Exception {
        while (true) {
            synchronized (this) {
                while (this.buffer.isEmpty()) {
                    wait();
                }
                Request_sol_Request.OraclersToRequestAssignedEventResponse _req
                        = this.buffer.remove(0); //removing the first element
                BigInteger _reqId = _req.requestId;
                Object[] _oraclersAdd = _req.oraclerAddress.toArray();
                Object[] myList = _req.from.toArray();
                Object[] myListTo = _req.to.toArray();
                for (int i = 0; i < myList.length; i++) {
                    if (!myList[i].toString().equals(EMPTY)) {
                        String privateKey = fetchPrivateKeyByAddress(myList[i].toString());
                        TransactionReceipt transactionReceipt = Transfer.sendFunds(
                                web3j,
                                getPrivateKeyFromAddress(privateKey),
                                myListTo[i].toString(),
                                BigDecimal.valueOf(1.0), Convert.Unit.ETHER).send();
                        System.out.println("Receipt : " + transactionReceipt.getBlockHash());
                    }
                }
                fetchRequestData(_reqId,
                        _oraclersAdd,
                        this.responseContract,
                        this.requestContract
                );
                notify();
            }
        }
    }

    private void fetchRequestData1(
            BigInteger _requestId,
            Object[] _candidateOraclers,
            Response_sol_Response _deployedResponseContract,
            Request_sol_Request _deployedRequestContract
    ) throws Exception {
        List<Thread> threads = new ArrayList<Thread>();
        double duration = 0;
        double currentTime = System.currentTimeMillis();
        for (int i = 0; (i < _candidateOraclers.length); i++) {
            final String myOracle = _candidateOraclers[i].toString();
            Thread thread = new Thread() {
                public void run() {
                    String _oracler = myOracle;// _candidateOraclers[i].toString();
//            byte[] response = stringToByte(randomStringGenerator(32));
                    byte[] request = null;
                    String content = null;
                    try {
                        request = _deployedRequestContract.
                                getRequestQuery(_requestId).send();
                        content = getUrlContent(INTERPREDATA, new String(request));
                    } catch (IOException ex) {
                        Logger.getLogger(RequestHandler.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (Exception ex) {
                        Logger.getLogger(RequestHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    double duration = System.currentTimeMillis();
                    double diff = duration - currentTime;
                    System.out.println(new String(request) + " >>>>>>>>>>>>> "
                            + content + " "
                            + currentTime + " "
                            //+ duration + " "
                            + diff + " "
                            + diff / 1000
                    );
                    byte[] response = stringToByte(content);
                    try {
                        _deployedResponseContract.createResponse(
                                _oracler,
                                response,
                                _requestId
                        ).sendAsync();
                    } catch (Exception ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }

//                    System.out.println("Thread Running");
                }
            };

            thread.start();
            threads.add(thread);
        }
        for (Thread t : threads) {
            t.join();
        }
        BigInteger responseId = _deployedResponseContract.whatIsRequestResponseId(_requestId).send();
        System.out.println("Finalizing response for id : " + responseId.intValue() + " ");
        _deployedResponseContract.processResponse(responseId).send();//Async(); working
    }
}
