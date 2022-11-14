// SPDX-License-Identifier: GPL-3.0-or-later
pragma solidity >=0.6.0;
import "./Oracle.sol";
import "./Request.sol";

contract Response {
    Oracle oracle;
    Request request;
    address oracleManager;
    uint256 nextResponseId;
    uint256[] private responseMappingIndex;
    uint256 timeWindow;
    bytes32 reward = bytes32("R");
    bytes32 penalty = bytes32("P");
    bytes32 one = bytes32("1");
    bytes32 zero = bytes32("0");

    //Prepare the time window for responses
    constructor() {
        timeWindow = 10; //in second
    }

    //Response struct for holding the object
    struct Responses {
        uint256 responseId;
        uint256 requestId;
        uint256 leastArrivalTime;
        uint256[] arrivalTime;
        bytes32[] response;
        address[] oraclerAddress;
        bytes32 outcome;
        bool hasOutcome;
    }

    //Oracle outcome struct for holding the object
    struct Outcome {
        uint256 timeArrival;
        bytes32 response;
        address owner;
        bytes32 mode;
        uint256 stake;
    }

    //Event for creation of a response
    event responseCreated(uint256 requestId, address _address);
    //Event for finalizing a resposen
    event responseFinalized(uint256 responseId, bytes32 outcome, address owner);

    //An array of response for requests
    Responses[] responsesSet;
    //Assigning ids to responses
    mapping(uint256 => Responses) public responses;

    //Set manager address
    function setOraclerManagerAddress() public {
        oracleManager = oracle.getOracleManagerAddress();
    }

    //Set the deployed oracle contract
    function accessDeployedOracleContract(address deployedOracle)
        public
        returns (bool)
    {
        oracle = Oracle(deployedOracle);
        setOraclerManagerAddress();
        return true;
    }

    //Set the deployed oracle contract
    function accessDeployedRequestContract(address deployedRequest)
        public
        returns (bool)
    {
        request = Request(deployedRequest);
        return true;
    }

    //Get a request either pending/non-pending
    function activeResponse(uint256 _responseId)
        public
        view
        returns (
            uint256 responseId,
            uint256 requestId,
            uint256[] memory arrivalTime,
            bytes32[] memory response,
            address[] memory oraclerAddress,
            bool hasOutcome
        )
    {
        require(
            isResponseCreated(_responseId) == true,
            "There is no request with the specified id."
        );
        return (
            responsesSet[_responseId].responseId,
            responsesSet[_responseId].requestId,
            responsesSet[_responseId].arrivalTime,
            responsesSet[_responseId].response,
            responsesSet[_responseId].oraclerAddress,
            responsesSet[_responseId].hasOutcome
        );
    }

    //Check the oracler is valid, not the manager.
    function senderIsValidOracler(address _sender) public view returns (bool) {
        address[] memory _oraclers = oracle.registeredOraclersAddress();
        bool result = false;
        for (uint256 i = 0; i < _oraclers.length; i++) {
            if (_oraclers[i] == _sender) {
                result = true;
                break;
            }
        }
        return result;
    }

    //Create a response out of an oracler's feedback
    function createResponse(
        address oraclerAddress,
        bytes32 response,
        uint256 requestId
    ) public {
        require(
            senderIsValidOracler(oraclerAddress) == true,
            "Assigned oracler must return a response."
        );
        // bytes32 byteResponse = bytes32(response);
        require(response.length > 0, "Return response cannot be empty.");
        (bool _existance, uint256 _responseId, bool _responder) =
            requestHasResponse(requestId, oraclerAddress);
        require(
            _responder == false,
            "Oracler has already responded to the request."
        );
        if (!_existance) {
            Responses storage _response = responses[nextResponseId];
            _response.arrivalTime.push(block.timestamp);
            _response.oraclerAddress.push(oraclerAddress);
            _response.requestId = requestId;
            _response.response.push(response);
            _response.responseId = nextResponseId;
            _response.hasOutcome = false;
            _response.leastArrivalTime = block.timestamp;
            responsesSet.push(_response);
            nextResponseId++;
        } else {
            Responses storage _response = responses[_responseId];
            _response.arrivalTime.push(block.timestamp);
            responsesSet[_responseId].arrivalTime.push(block.timestamp);
            _response.oraclerAddress.push(oraclerAddress);
            responsesSet[_responseId].oraclerAddress.push(oraclerAddress);
            _response.response.push(response);
            responsesSet[_responseId].response.push(response);
            _response.hasOutcome = false;
            responsesSet[_responseId].hasOutcome = false;
            if (_response.leastArrivalTime > block.timestamp) {
                _response.leastArrivalTime = block.timestamp;
                responsesSet[_responseId].leastArrivalTime = block.timestamp;
            }
        }

        emit responseCreated(requestId, oraclerAddress);
    }

    //Get the first response arrival time
    function getResponseLeastArrivalTime(uint256 _responseId)
        public
        view
        returns (uint256 _leastArrivalTime)
    {
        Responses memory _response = responsesSet[_responseId];
        return _response.leastArrivalTime;
    }

    //Return all the corresponding arrival times
    function getResponsesArrivalTimes(uint256 _responseId)
        public
        view
        returns (uint256[] memory times)
    {
        Responses memory _response = responsesSet[_responseId];
        return _response.arrivalTime;
    }

    //Process the response and return the final outcome
    function processResponse(uint256 _responseId) public {
        Responses memory _response = responsesSet[_responseId];
        uint256 _count = _response.response.length;
        Outcome[] memory outcomes = new Outcome[](_count);
        // uint256 _initialResponse = _response.arrivalTime[0]; //the first response
        uint256[] memory _requestStakes =
            request.getRequestStakes(_response.requestId);
        uint256 _index;
        uint256 _ones;
        uint256 _zeros;
        uint256 winner;
        bool winnerStatus = true; //true 1s are winner, otherwise, 0s are winner.
        uint256[] memory _onesIndex = new uint256[](_count);
        uint256[] memory _zerosIndex = new uint256[](_count);
        for (uint256 i = 0; i < _count; i++) {
            if (
                _response.arrivalTime[i] - _response.leastArrivalTime <=
                timeWindow
            ) {
                //
                Outcome memory _tempOutcome =
                    Outcome(
                        _response.arrivalTime[i],
                        _response.response[i],
                        _response.oraclerAddress[i],
                        reward,
                        _requestStakes[_index]
                    );
                if (outcomes[i].response == one) {
                    _onesIndex[_ones] = i;
                    _ones = _ones + 1;
                } else if (outcomes[i].response == zero) {
                    _zerosIndex[_zeros] = i;
                    _zeros = _zeros + 1;
                }
                outcomes[_index] = _tempOutcome;
            } else {
                Outcome memory _tempOutcome =
                    Outcome(
                        _response.arrivalTime[i],
                        _response.response[i],
                        _response.oraclerAddress[i],
                        penalty,
                        _requestStakes[_index]
                    );
                outcomes[_index] = _tempOutcome;
            }
            _index++;
        }
        if (_ones >= _zeros) {
            winner = _ones;
            winnerStatus = true;
            for (uint256 i = 0; i < _zeros; i++) {
                outcomes[_zerosIndex[i]].mode = penalty;
            }
        } else {
            winner = _zeros;
            winnerStatus = false;
            for (uint256 i = 0; i < _ones; i++) {
                outcomes[_onesIndex[i]].mode = penalty;
            }
        }
        sortResponseOutcomeByTimeArrival(outcomes);
        applyRewardOrPenaltyOutcome(outcomes);
        uint256 winnerIndex = getResponseMedian(winner);
        if (winnerStatus == true) {
            winnerIndex = _onesIndex[winnerIndex];
        } else {
            winnerIndex = _zerosIndex[winnerIndex];
        }
        // uint256 _median = getResponseMedian(winner);//_count
        setResponseHasOutcome(
            true,
            _responseId,
            outcomes[winnerIndex].response
        ); //outcomes[_median].response

        emit responseFinalized(
            _responseId,
            outcomes[winnerIndex].response,
            outcomes[winnerIndex].owner
        );
    }

    //Penalize or reward the participating oracles
    //Changing their popularity and balance
    //Utilize the popularity real value
    function applyRewardOrPenaltyOutcome(Outcome[] memory outcomes) private {
        for (uint256 i = 0; i < outcomes.length; i++) {
            uint256 _index = oracle.oraclerMappingIndex(outcomes[i].owner);
            uint256 _popularity = oracle.getOraclerNumericPopularity(_index);
            uint256 _stake = outcomes[i].stake;
            uint256 value = 1;
            if (outcomes[i].mode == bytes32("R")) {
                value += _popularity;
                if (value > 100) {
                    value = 100;
                }
                // value = mostSignificantBit(value);
                oracle.increaseOraclerBalance(outcomes[i].owner, _stake);
                oracle.changeOraclerPopularity(
                    outcomes[i].owner,
                    _index,
                    mostSignificantBit(value),
                    value
                );
            } else {
                value = _popularity - value;
                if (value < 0) {
                    value = 0;
                }
                // value = mostSignificantBit(value);
                oracle.decreaseOraclerBalance(outcomes[i].owner, _stake);
                oracle.changeOraclerPopularity(
                    outcomes[i].owner,
                    _index,
                    mostSignificantBit(value),
                    value
                );
            }
        }
    }

    //Deprecated function; it was supposed to consider
    //the current popularity for arragment.
    function applyRewardOrPenalty(
        address[] memory oraclers,
        bytes32[] memory mode
    ) public {
        for (uint256 i = 0; i < oraclers.length; i++) {
            uint256 _index = oracle.oraclerMappingIndex(oraclers[i]);
            uint256 _popularity = oracle.getOraclerNumericPopularity(_index);
            uint256 value = 1;
            if (mode[i] == bytes32("R")) {
                value += _popularity;
                if (value > 100) {
                    value = 100;
                }
                // value = mostSignificantBit(value);
                oracle.changeOraclerPopularity(
                    oraclers[i],
                    _index,
                    mostSignificantBit(value),
                    value
                );
            } else {
                value = _popularity - value;
                if (value < 0) {
                    value = 0;
                }
                // value = mostSignificantBit(value);
                oracle.changeOraclerPopularity(
                    oraclers[i],
                    _index,
                    mostSignificantBit(value),
                    value
                );
            }
        }
    }

    //Gets corresponding median value
    function getResponseMedian(uint256 _sortedResponseSize)
        public
        pure
        returns (uint256 medianValue)
    {
        // uint256 value = _sortedResponseSize % 2;
        uint256 _median = _sortedResponseSize / 2;
        // if (value == 0) {//value != 0
        //     _median = _median + 1;
        // }
        return (_median);
    }

    //Sort responses based on their arrival times
    //Consider the array of oject
    function sortResponseOutcomeByTimeArrival(Outcome[] memory outcome)
        public
        pure
    {
        for (uint256 i = 0; i < outcome.length; i++) {
            Outcome memory iOutcome = outcome[i];
            for (uint256 j = i + 1; j < outcome.length; j++) {
                Outcome memory jOutcome = outcome[j];
                if (iOutcome.timeArrival > jOutcome.timeArrival) {
                    Outcome memory temp = iOutcome;
                    outcome[i] = jOutcome;
                    outcome[j] = temp;
                }
            }
        }
    }

    //Return sorted responses in an array like manner
    //For external use
    function sortResponseTimeArrival(
        bytes32[] memory _response,
        address[] memory _owner,
        uint256[] memory _timeArrivals,
        uint256[] memory _rewardStakes
    )
        public
        pure
        returns (
            bytes32[] memory response,
            address[] memory owners,
            uint256[] memory timeArrivals,
            uint256[] memory rewardStakes
        )
    {
        for (uint256 i = 0; i < _timeArrivals.length; i++) {
            for (uint256 j = i + 1; j < _timeArrivals.length; j++) {
                if (_timeArrivals[i] > _timeArrivals[j]) {
                    uint256 temp = _timeArrivals[i];
                    uint256 tempStake = _rewardStakes[i];
                    address tempAdd = _owner[i];
                    bytes32 tempResp = _response[i];
                    _timeArrivals[i] = _timeArrivals[j];
                    _rewardStakes[i] = _rewardStakes[j];
                    _owner[i] = _owner[j];
                    _response[i] = _response[j];
                    _timeArrivals[j] = temp;
                    _rewardStakes[j] = tempStake;
                    _owner[j] = tempAdd;
                    _response[j] = tempResp;
                }
            }
        }
        return (_response, _owner, _timeArrivals, _rewardStakes);
    }

    //Check the request has a response
    //also checks whether a specific address has already reponded or not
    function requestHasResponse(uint256 requestId, address oraclerAddress)
        public
        view
        returns (
            bool value,
            uint256 responseId,
            bool responder
        )
    {
        require(
            msg.sender == oraclerAddress || msg.sender == oracleManager,
            "Only manager or the response creator has access to it."
        );
        value = false;
        responder = false;
        if (responsesSet.length > 0) {
            for (uint256 i = 0; i < responsesSet.length; i++) {
                Responses memory _response = responsesSet[i];
                if (_response.requestId == requestId) {
                    value = true;
                    responseId = _response.responseId;
                    for (
                        uint256 j = 0;
                        j < _response.oraclerAddress.length;
                        j++
                    ) {
                        if (_response.oraclerAddress[j] == oraclerAddress) {
                            responder = true;
                            break;
                        }
                    }
                    break;
                }
            }
        }
        return (value, responseId, responder);
    }

    //Check wether there is such a response or not
    function isResponseCreated(uint256 _responseId) public view returns (bool) {
        for (uint256 i = 0; i < responsesSet.length; i++) {
            if (responsesSet[i].responseId == _responseId) {
                return true;
            }
        }
        return false;
    }

    //Check wether there is such a response or not
    function whatIsRequestResponseId(uint256 _requestId)
        public
        view
        returns (uint256 id)
    {
        for (uint256 i = 0; i < responsesSet.length; i++) {
            if (responsesSet[i].requestId == _requestId) {
                return responsesSet[i].responseId;
            }
        }
    }

    //Check the response status
    function responseIsFinalized(uint256 _responseId)
        public
        view
        returns (bool)
    {
        return responsesSet[_responseId].hasOutcome;
    }

    //Set the oracle final outcome for a request
    function setResponseHasOutcome(
        bool _hasOutcome,
        uint256 _responseId,
        bytes32 _outcome
    ) private {
        responsesSet[_responseId].outcome = _outcome;
        responsesSet[_responseId].hasOutcome = _hasOutcome;
    }

    //Set the timewindow for receiving the responses
    function setResponseTimeWindow(uint256 _window) public {
        require(_window > 0, "Time window must be positive.");
        timeWindow = _window;
    }

    //Get the response window
    function getResponseTimeWindow() public view returns (uint256) {
        return timeWindow;
    }

    //Control popularity
    //It returns the most significant bit for popularity management
    // x >>= 1 menas x = x >> 1, and '>>' menas shift.
    //Check out https://docs.soliditylang.org/en/latest/types.html#shifts
    function mostSignificantBit(uint256 x) public pure returns (uint256 r) {
        require(x > 0);

        if (x >= 0x100000000000000000000000000000000) {
            x >>= 128;
            r += 128;
        }
        if (x >= 0x10000000000000000) {
            x >>= 64;
            r += 64;
        }
        if (x >= 0x100000000) {
            x >>= 32;
            r += 32;
        }
        if (x >= 0x10000) {
            x >>= 16;
            r += 16;
        }
        if (x >= 0x100) {
            x >>= 8;
            r += 8;
        }
        if (x >= 0x10) {
            x >>= 4;
            r += 4;
        }
        if (x >= 0x4) {
            x >>= 2;
            r += 2;
        }
        if (x >= 0x2) r += 1; // No need to shift x anymore
    }
}
