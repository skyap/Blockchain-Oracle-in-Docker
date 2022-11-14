package OracleProject;
import io.reactivex.Scheduler;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.web3j.abi.EventEncoder;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthAccounts;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.tuples.generated.Tuple6;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Convert;

/**
 *
 * @author Amir
 */
public class Main {

    private static Web3j web3j;
    private static Web3ClientVersion web3ClientVersion;
    private static TransactionManager transactionManager;
    private static EthAccounts ethAccounts;
    private static EthGetBalance ethGetBalance;
    private static List<String> ethAccountList;
    private static List<String> ptInfo;
    private static List<AccountContainer> accountContainers;
    private static Initializer initializer;
    private static Credentials ORACLE_MANAGER_CREDENTIALS = null;
    private static ContractGasProvider contractGasProvider;
    private static String ORACLE_MANAGER_ADDRESS = "";
    private static final int ORACLE_MANAGER_INDEX = 0;
    private static String DEPLOYED_ORACLE_ADDRESS = "";
    private static String DEPLOYED_REQUEST_ADDRESS = "";
    private static String DEPLOYED_RESPONSE_ADDRESS = "";
    private static BufferedReader br;
    private static SecureRandom secureRandom;

    private static final BigInteger GAS_LIMIT = BigInteger.valueOf(6721975L);
    private static final BigInteger GAS_PRICE = BigInteger.valueOf(20000000000L);
    private static final String DEPLOYED_CONTRACT_ADDRESS_FILE = "deploy.txt";
    private static final String DEPLOYED_REQUEST_ADDRESS_FILE = "request.txt";
    private static final String DEPLOYED_RESPONSE_ADDRESS_FILE = "response.txt";
    private static final String ORACLER = "Oracler";
    private static final String COMMA = ",";
    private static final String EMPTY = "0x0000000000000000000000000000000000000000";
    private static Object condition = false;
    private final static String URL = "https://min-api.cryptocompare.com/data/price?fsym=ETH&tsyms=BTC,USD,EUR";
    private final static String INTERPREDATA = "https://tracebaseapi.azurewebsites.net/BeefValidation/";
    protected static List<Request> requestList = new ArrayList<>();

    public Main(List<Request> reqList) {
        this.requestList = reqList;
    }
    
    private static void writeOutcomeTimeIntoTheFile(String fileName, String gas){
        FileOutputStream gasWriter;
        DataOutputStream dos;
        try {
            gasWriter = new FileOutputStream(fileName, true);
            dos = new DataOutputStream(gasWriter);
            dos.writeBytes(gas);
            dos.writeBytes(",\n");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        
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

    private static void printAccountInformation() {

        for (AccountContainer account : accountContainers) {
            System.out.println(account.getAccountAddress() + " "
                    + account.getAccountPrivateKey() + " "
                    + account.getAccountBalance() + " "
                    + account.getAccountToken()
            );
        }
    }

    private static Credentials getPrivateKeyFromAddress(String address) {
        return Credentials.create(address);
    }

    private static void getAccountInformation(
            Web3j web3j,
            EthAccounts ethAccounts,
            Initializer initializer
    ) throws IOException {

        List<String> accountList = ethAccounts.getAccounts();
        List<String> privateKeys = initializer.fetchGanachPrivateKeys();
        ORACLE_MANAGER_ADDRESS = accountList.get(0); //Manager index

        for (int i = 0; i < accountList.size(); i++) {
            String address = accountList.get(i);
            EthGetBalance ethGetBalance = web3j.
                    ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
            BigDecimal balance = Convert.fromWei(ethGetBalance.getBalance().toString(), Convert.Unit.ETHER);
            accountContainers.add(new AccountContainer(ethAccounts.getAccounts().get(i),
                    privateKeys.get(i),
                    balance,
                    Convert.Unit.ETHER
            ));
        }
    }

    /*
    * Extract component objects
     */
    public static List<ArrayList> fetchTupleComponents(
            Object tuple,
            int numOfTuples
    ) {
        List<ArrayList> myTuple = new ArrayList<>();
        switch (numOfTuples) {
            case 6:
                Tuple6 tuple6 = (Tuple6) tuple;
                myTuple.add((ArrayList) tuple6.component1());
                myTuple.add((ArrayList) tuple6.component2());
                myTuple.add((ArrayList) tuple6.component3());
                myTuple.add((ArrayList) tuple6.component4());
                myTuple.add((ArrayList) tuple6.component5());
                myTuple.add((ArrayList) tuple6.component6());
                break;
            default:
                System.out.println("Error occured during tuple interpretation.");
        }

        return myTuple;
    }

    public static List<Oracler> buildOraclerStruct(
            List<ArrayList> oracler,
            Oracle_sol_Oracle deployedContract
    ) {
        List<Oracler> oraclerList = new ArrayList<>();

        List<byte[]> names = new ArrayList<>();
        List<String> addresses = new ArrayList<>();
        List<BigInteger> poupularity = new ArrayList<>();
        List<Boolean> status = new ArrayList<>();
        List<BigInteger> balance = new ArrayList<>();
        List<BigInteger> ids = new ArrayList<>();
        int item = oracler.get(0).size();

        names = oracler.get(0);
        addresses = oracler.get(1);
        poupularity = oracler.get(2);
        status = oracler.get(3);
        balance = oracler.get(4);
        ids = oracler.get(5);

        for (int i = 0; i < item; i++) {
            oraclerList.add(new Oracler(
                    names.get(i),
                    addresses.get(i),
                    poupularity.get(i),
                    status.get(i),
                    balance.get(i),
                    ids.get(i),
                    deployedContract,
                    web3j
            ));
        }

        return oraclerList;
    }

    public static void printOraclerStruct(Oracler _oracler) {
        System.out.println(_oracler.toString());
    }

    /*
    * Extract component objects
     */
    public static List<ArrayList> fetchResponseTupleComponents(
            Object tuple,
            int numOfTuples
    ) {
        List myTuple = new ArrayList<>();
        switch (numOfTuples) {
            case 6:
                Tuple6 tuple6 = (Tuple6) tuple;
                myTuple.add((BigInteger) tuple6.component1());
                myTuple.add((BigInteger) tuple6.component2());
                myTuple.add((ArrayList) tuple6.component3());
                myTuple.add((ArrayList) tuple6.component4());
                myTuple.add((ArrayList) tuple6.component5());
                myTuple.add((boolean) tuple6.component6());
                break;
            default:
                System.out.println("Error occured during tuple interpretation.");
        }

        return myTuple;
    }

    public static List<Response> buildResponseStruct(
            List request,
            Response_sol_Response deployedContract
    ) {
        List<Response> responseList = new ArrayList<>();

        BigInteger responseId = (BigInteger) request.get(0);
        BigInteger requestId = (BigInteger) request.get(1);
        ArrayList<BigInteger> arrivalTimes = (ArrayList<BigInteger>) request.get(2);
        ArrayList<byte[]> responses = (ArrayList<byte[]>) request.get(3);
        ArrayList<String> address = (ArrayList<String>) request.get(4);
        boolean outcome = (boolean) request.get(5);

        responseList.add(new Response(
                responseId,
                requestId,
                arrivalTimes,
                responses,
                address,
                outcome,
                deployedContract
        ));

        return responseList;
    }

    private static String deployOracleContract(
            Web3j _web3j,
            TransactionManager _transactionManager,
            ContractGasProvider _contractGasProvider
    ) {
        String deployedContractAddress = "";
        BufferedWriter bw;
        try {
            bw = new BufferedWriter(new FileWriter(DEPLOYED_CONTRACT_ADDRESS_FILE));
            deployedContractAddress = Oracle_sol_Oracle.deploy(
                    _web3j,
                    _transactionManager,
                    _contractGasProvider
            ).send().getContractAddress();
            bw.write(deployedContractAddress);
            bw.close();
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        return deployedContractAddress;
    }

    private static String deployRequestContract(
            Web3j _web3j,
            TransactionManager _transactionManager,
            ContractGasProvider _contractGasProvider
    ) {
        String deployedContractAddress = "";
        BufferedWriter bw;
        try {
            bw = new BufferedWriter(new FileWriter(DEPLOYED_REQUEST_ADDRESS_FILE));
            deployedContractAddress = Request_sol_Request.deploy(
                    _web3j,
                    _transactionManager,
                    _contractGasProvider
            ).send().getContractAddress();
            bw.write(deployedContractAddress);
            bw.close();
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        return deployedContractAddress;
    }

    private static String deployResponseContract(
            Web3j _web3j,
            TransactionManager _transactionManager,
            ContractGasProvider _contractGasProvider
    ) {
        String deployedContractAddress = "";
        BufferedWriter bw;
        try {
            bw = new BufferedWriter(new FileWriter(DEPLOYED_RESPONSE_ADDRESS_FILE));
            deployedContractAddress = Response_sol_Response.deploy(
                    _web3j,
                    _transactionManager,
                    _contractGasProvider
            ).send().getContractAddress();
            bw.write(deployedContractAddress);
            bw.close();
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        return deployedContractAddress;
    }

    private static Oracle_sol_Oracle loadOracleContract(
            Web3j _web3j,
            TransactionManager _transactionManager,
            ContractGasProvider _contractGasProvider
    ) {
        try {
            br = new BufferedReader(new FileReader(DEPLOYED_CONTRACT_ADDRESS_FILE));
            DEPLOYED_ORACLE_ADDRESS = br.readLine();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Oracle_sol_Oracle.load(DEPLOYED_ORACLE_ADDRESS,
                _web3j,
                _transactionManager,
                _contractGasProvider
        );
    }

    private static Request_sol_Request loadRequestContract(
            Web3j _web3j,
            TransactionManager _transactionManager,
            ContractGasProvider _contractGasProvider
    ) {
        try {
            br = new BufferedReader(new FileReader(DEPLOYED_REQUEST_ADDRESS_FILE));
            DEPLOYED_REQUEST_ADDRESS = br.readLine();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Request_sol_Request.load(DEPLOYED_REQUEST_ADDRESS,
                _web3j,
                _transactionManager,
                _contractGasProvider
        );
    }

    private static Response_sol_Response loadResponseContract(
            Web3j _web3j,
            TransactionManager _transactionManager,
            ContractGasProvider _contractGasProvider
    ) {
        try {
            br = new BufferedReader(new FileReader(DEPLOYED_RESPONSE_ADDRESS_FILE));
            DEPLOYED_RESPONSE_ADDRESS = br.readLine();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Response_sol_Response.load(DEPLOYED_RESPONSE_ADDRESS,
                _web3j,
                _transactionManager,
                _contractGasProvider
        );
    }

    public static EthFilter createContractFilter(String contractAddress, String topic) {
        EthFilter oracleFilter = new EthFilter(
                DefaultBlockParameterName.LATEST,
                DefaultBlockParameterName.LATEST,
                contractAddress
        );
        oracleFilter.addSingleTopic(topic);
        return oracleFilter;
    }

    public static void printOracleInformation(Oracle_sol_Oracle deployedOracle) throws Exception {
        List<BigInteger> oraclerIndex = deployedOracle.mappingOraclersIndex().send();
//        System.out.println(deployedOracle.getCountRegisteredOraclers().send());
        Tuple6 myTuple = deployedOracle.registeredOraclers(oraclerIndex).send();
        List<ArrayList> map = fetchTupleComponents(myTuple, 6);
        List<Oracler> myOracler = buildOraclerStruct(map, deployedOracle);
        for (Oracler orc : myOracler) {
            printOraclerStruct(orc);
        }
    }

    public static void printResponseInformation(Response_sol_Response deployedResponse) throws Exception {
        Tuple6 myTuple = deployedResponse.activeResponse(BigInteger.ZERO).send();
        List<ArrayList> map = fetchResponseTupleComponents(myTuple, 6);
        List<Response> myResponse = buildResponseStruct(map, deployedResponse);
        for (Response resp : myResponse) {
            printResponseStruct(resp);
        }
    }

    public static void printResponseStruct(Response _response) {
        System.out.println(_response.toString());
    }

    public static String fetchPrivateKeyByAddress(String address) {
        String prvKey = null;
        for (AccountContainer ac : accountContainers) {
            if (ac.getAccountAddress().equals(address)) {
                prvKey = ac.getAccountPrivateKey();
                break;
//                return ac.getAccountPrivateKey();
            }
        }
        return prvKey;
    }

    /**
     * @param args the command line arguments
     */

    public static void main(String[] args) throws Exception {
        // TODO code application logic here


        web3j = Web3j.build(new HttpService());
        accountContainers = new ArrayList<>();
        initializer = new Initializer();
        contractGasProvider = new StaticGasProvider(GAS_PRICE, GAS_LIMIT);
        secureRandom = new SecureRandom();
        try {

            web3ClientVersion = web3j.web3ClientVersion().send();
            ethAccounts = web3j.ethAccounts().send();
            ethAccountList = ethAccounts.getAccounts();
            getAccountInformation(web3j, ethAccounts, initializer);
//            ptInfo = initializer.fetchPTInformation();

            RequestContractBooter requestContractBooter = new RequestContractBooter(ptInfo, requestList);
//            printAccountInformation();
            ORACLE_MANAGER_CREDENTIALS = getPrivateKeyFromAddress(
                    accountContainers.get(ORACLE_MANAGER_INDEX).getAccountPrivateKey());

            System.out.println(ORACLE_MANAGER_CREDENTIALS.getAddress());

        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        String web3jClientVersion = web3ClientVersion.getWeb3ClientVersion();
        System.out.println("Current Web3jClinetVersion is " + web3jClientVersion);

        transactionManager = new RawTransactionManager(
                web3j,
                ORACLE_MANAGER_CREDENTIALS
        );

        DEPLOYED_ORACLE_ADDRESS = deployOracleContract(
                web3j,
                transactionManager,
                contractGasProvider
        );
        System.out.println("The deployed ORACLE contract address is " + DEPLOYED_ORACLE_ADDRESS);

        DEPLOYED_REQUEST_ADDRESS = deployRequestContract(
                web3j,
                transactionManager,
                contractGasProvider
        );
        System.out.println("The deployed REQUEST contract address is " + DEPLOYED_REQUEST_ADDRESS);

        DEPLOYED_RESPONSE_ADDRESS = deployResponseContract(
                web3j,
                transactionManager,
                contractGasProvider
        );
        System.out.println("The deployed RESPONSE contract address is " + DEPLOYED_RESPONSE_ADDRESS);

        Oracle_sol_Oracle deployedOracle = loadOracleContract(web3j,
                transactionManager,
                contractGasProvider
        );
        String deployedContractAddress = deployedOracle.getContractAddress();
        System.out.println("The ORACLE contract has been deployed at "
                + deployedContractAddress + " "
                + DEPLOYED_ORACLE_ADDRESS
        );

        Request_sol_Request deployedRequest = loadRequestContract(web3j,
                transactionManager,
                contractGasProvider
        );
        String deployedRequestContractAddress = deployedRequest.getContractAddress();
        System.out.println("The REQUEST contract has been deployed at "
                + deployedRequestContractAddress + " "
                + DEPLOYED_REQUEST_ADDRESS
        );

        Response_sol_Response deployedResponse = loadResponseContract(web3j,
                transactionManager,
                contractGasProvider
        );
        String deployedResponseContractAddress = deployedResponse.getContractAddress();
        System.out.println("The RESPONSE contract has been deployed at "
                + deployedResponseContractAddress + " "
                + DEPLOYED_RESPONSE_ADDRESS
        );

        deployedResponse.accessDeployedOracleContract(
                deployedContractAddress
        ).send();
        deployedResponse.accessDeployedRequestContract(
                deployedRequestContractAddress
        ).send();

        int numOfOraclers = 5;
        Map<TransactionReceipt, String> transactionReceipts
                = initializer.createOraclers(deployedOracle,
                        initializer.oraclersAddress(accountContainers, numOfOraclers)
                );

        String oraclerAddress = "";
        for (Map.Entry<TransactionReceipt, String> tr : transactionReceipts.entrySet()) {
            final String value = tr.getValue();
            oraclerAddress += tr.getValue() + "\n";
        }
        System.out.println(oraclerAddress);

        EthFilter oracleFilter = createContractFilter(
                deployedOracle.getContractAddress(),
                EventEncoder.encode(Oracle_sol_Oracle.ORACLEISCREATED_EVENT)
        //                "oracleCreated"
        );

        EthFilter requestFilter = createContractFilter(
                deployedRequest.getContractAddress(),
                EventEncoder.encode(Request_sol_Request.REQUESTCREATED_EVENT)
        );

        EthFilter requestAssignedFilter = createContractFilter(
                deployedRequest.getContractAddress(),
                EventEncoder.encode(Request_sol_Request.ORACLERSTOREQUESTASSIGNED_EVENT)
        );

        EthFilter responseFilter = createContractFilter(
                deployedResponse.getContractAddress(),
                EventEncoder.encode(Response_sol_Response.RESPONSECREATED_EVENT)
        );

        EthFilter outcomeFilter = createContractFilter(
                deployedResponse.getContractAddress(),
                EventEncoder.encode(Response_sol_Response.RESPONSEFINALIZED_EVENT)
        );

        deployedOracle.oracleIsCreatedEventFlowable(oracleFilter).subscribe(
                reply -> {
                    System.out.println("An oracler registered."
                            + " "
                            + reply.oracler + " "
                            + reply.oraclerId + " "
                            + reply.log.toString()
                    );
//                    synchronized (condition) {
//                        condition = true;
//                        notify();
//                    }
                }
        );

        deployedRequest.requestCreatedEventFlowable(requestFilter).subscribe(
                reply -> {
                    byte[] byteValue = new byte[reply.barcode.length];
                    System.arraycopy(reply.barcode, 0, byteValue, 0, reply.barcode.length);
                    Request _req = new Request(
                            reply.requestId.intValue(),
                            System.currentTimeMillis(),
                            0
                    );
                    requestList.add(_req);
                    System.out.println("A request is submitted."
                            + " "
                            + new String(reply.barcode, Charset.forName("UTF-8")) + " with id "
                            + reply.requestId + " "
                    //                            + reply.log.toString()
                    );
                    deployedRequest.assignOraclersToRequest(reply.requestId).send();//Async();
                }
        );

        final RequestHandler requestHandler = new RequestHandler(
                deployedRequest,
                deployedResponse,
                accountContainers,
                web3j
        );

        deployedRequest.oraclersToRequestAssignedEventFlowable(requestAssignedFilter).subscribe(
                reply -> {
                    System.out.println("Oraclers are assigned to the request is submitted."
                            + " "
                            + reply.requestId + " "

                    );

                    printOracleInformation(deployedOracle);
                }
        );
        
        deployedResponse.responseCreatedEventFlowable(responseFilter).subscribe(
                reply -> {
//                    System.out.println("A response is submitted."
//                            + " "
//                            + reply._address + " "
//                            + reply.requestId + " "
////                            + reply.log.toString()
//                    );
                }
        );

        deployedResponse.responseFinalizedEventFlowable(outcomeFilter).subscribe(
                reply -> {
                    String fileName = "time_" + numOfOraclers +".txt";
                    int _id = reply.responseId.intValue();
                    requestList.get(_id).setFinishTime(System.currentTimeMillis());
                    double _time = (requestList.get(_id).getFinishTime() - requestList.get(_id).getArrivalTime());
                    writeOutcomeTimeIntoTheFile(fileName, Double.toString(_time));
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>A response is finalized."
                            + " "
                            + reply.responseId + " "
                            + new String(reply.outcome, Charset.forName("UTF-8")) + " "
                            + reply.owner
                    );
                }
        );
        Thread request = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    requestHandler.produce();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
        Thread response = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    requestHandler.consume();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Exception ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
        request.start();
        response.start();

        request.join();
        response.join();

    }

    private static String randomStringGenerator(int length) {
        final String chars = "0123456789abcdefghigklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVXYZ@#$%&*!";
//        secureRandom = new SecureRandom();
        StringBuilder stringBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            stringBuilder.append(
                    chars.charAt(secureRandom.nextInt(chars.length()))
            );
        }
        return stringBuilder.toString();
    }

    private static void fetchRequestData(
            BigInteger _requestId,
            Object[] _candidateOraclers,
            Response_sol_Response _deployedContract,
            Request_sol_Request _deployedRequest
    ) throws Exception {
        for (int i = 0; i < _candidateOraclers.length; i++) {
//            System.out.println(_candidateOraclers.get(i));
            String _oracler = _candidateOraclers[i].toString();
            byte[] request = _deployedRequest.
                    getRequestQuery(_requestId).send();
            System.out.println(new String(request));
            String content = getUrlContent(INTERPREDATA, new String(request));
            byte[] response = initializer.stringToByte(content);
            //ptInfo.get(secureRandom.nextInt(ptInfo.size())));
            System.out.println("Real Content is  " + content);
            //initializer.stringToByte(randomStringGenerator(32));
            try {
                _deployedContract.createResponse(
                        _oracler,
                        response,
                        _requestId
                ).send();
            } catch (Exception ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
