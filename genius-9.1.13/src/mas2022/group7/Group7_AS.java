package src.mas2022.group7;

import genius.core.BidHistory;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.*;
import genius.core.Bid;

import java.util.*;

public class Group7_AS extends AcceptanceStrategy {
    public List<Bid> acceptableBids = new ArrayList<>();
    public double acceptanceThreshold;
    public double slope;
    public double intercept;

    public List<Double> listUtilities = new ArrayList<>();
    public boolean IsAvgCalculated;

    public String DECLINE_START = "time to start declining";
    public String FIRST_UTILITY_VALUE = "first utility value";
    public String OPP_BIDS_AMOUNT = "amount of opponent bids used";

    public Set<BOAparameter> getParameterSpec() {
        Set<BOAparameter> params = new HashSet<>();
        // From 0.6 time the decline starts happening from 0.95
        params.add(new BOAparameter(DECLINE_START, 0.6D, "time to start declining"));
        params.add(new BOAparameter(FIRST_UTILITY_VALUE, 0.95D, "first utility value"));
        params.add(new BOAparameter(OPP_BIDS_AMOUNT, 5D, "amount of opponent bids used"));
        return params;
    }

    @Override
    public String getName() {
        return "2022 - Agent 007 Acceptance Strategy";
    }

    @Override
    public Actions determineAcceptability() {
        Map<String, Double> params = this.getParameters();
        BidHistory oppBidHistory = this.negotiationSession.getOpponentBidHistory();
        List<BidDetails> allOppBids = oppBidHistory.getHistory();

        SortedOutcomeSpace outcomeSpace = new SortedOutcomeSpace(this.negotiationSession.getUtilitySpace());
        List<BidDetails> allPossibleBids = outcomeSpace.getOrderedList();

        // make the utility prediction of the opponents bids after certain time is passed so that the line becomes more reliable.
        double sum = 0;
        double sumIndexes = 0;
        if (this.negotiationSession.getTime() > params.get(DECLINE_START)) {
            for(BidDetails oppBid : allOppBids){
                double util = oppBid.getMyUndiscountedUtil();
                listUtilities.add(util);
                sum += util;
                sumIndexes += allOppBids.indexOf(oppBid);
            }
            // least squares regression to find the best fit of a line through the utilities of the opponent.
            double avgY = sum / allOppBids.size(); // avg y = avg utility
            double avgX = sumIndexes / allOppBids.size(); // avg x = avg of the sum of indexes

            double sumNumerator = 0;
            double sumDenominator = 0;
            for (int i = 0; i < this.listUtilities.size(); i++) {
                // i, in this case, is x
                sumNumerator += (i - avgX) * (listUtilities.get(i) - avgY); // x - x_avg * y - y_avg
                sumDenominator += (i - avgX) * (i - avgX); // x-x_avg * x-x_avg
            }

            // now we have a slope for our linear fit
            // slope = sum((x-xa)(y-ya))/ sum((x-xa)(x-xa))
            slope = sumNumerator / sumDenominator;
            // get intercept y_avg = slope * x_avg + b
            intercept = avgY - (slope * avgX);
            // line becomes y = slope * x + intercept
        }

        // Part 1 - Acceptability starts as a line and after a certain time, starts declining parabolically.
        if (this.negotiationSession.getTime() < params.get(DECLINE_START)){
            acceptanceThreshold = params.get(FIRST_UTILITY_VALUE);
            // the average utility is only needed after the time it starts declining. so whether it is calculated is set to false;
            IsAvgCalculated = false;
        }
        else {
            BidDetails oppBestBid = oppBidHistory.getBestBidDetails();
            double oppBestBidUtil = oppBestBid.getMyUndiscountedUtil();
            // estimate of total amount of bids the opponent will place in the whole negotiation
            double estimateTotalOppBids = allOppBids.size() / this.negotiationSession.getTime();

            double sumAllBidsUtil = 0;
            double avgAllUtil = 0;
            if (!IsAvgCalculated) {
                for (BidDetails bid : allPossibleBids) {
                    sumAllBidsUtil += this.negotiationSession.getUtilitySpace().getUtility(bid.getBid());
                }
                avgAllUtil = sumAllBidsUtil / allPossibleBids.size();
                IsAvgCalculated = true;
            }

            // the predicted utility of the opponents last bid;
            // use line created in the previous section
            // line becomes y = slope * x + intercept
            double predicted = slope * estimateTotalOppBids + intercept;
            // if the prediction of the last point is higher than the utility of the opponents highest bid then use it as an endpoint.
            // predicted = acceptUtil - (time left) * decline ^ 2
            // endpoint is the point of lowest acceptability utility at the end of the time
            double endpoint = Math.max(predicted, oppBestBidUtil);

            // failsafe since you don't want it to go lower than the average of all bids and this depends a lot on the domain
            if (endpoint < avgAllUtil){
                endpoint = avgAllUtil;
            }
            // failsafe if the endpoint is higher than acceptUtil then it should go up, but instead we will let it stay the same
            if (endpoint > acceptanceThreshold){
                endpoint = acceptanceThreshold;
            }

            // this decline rate will make sure that the accept util will finish at the endpoint at the end of time.
            double declineRate = Math.sqrt((endpoint - acceptanceThreshold) / - (1- this.negotiationSession.getTime()));

            double interval = this.negotiationSession.getTime() / allOppBids.size();
            acceptanceThreshold -= (interval * Math.pow(declineRate, 2));
        }

        for (BidDetails bid : allPossibleBids) {
            Bid nextBid = bid.getBid();
            if (this.negotiationSession.getUtilitySpace().getUtility(nextBid) > acceptanceThreshold) {
                acceptableBids.add(nextBid);
            }
        }

        // acceptance conditions:
        // opponent's bid has >= utility then the acceptance utility threshold
        // opponent's bid has >= utility then our next bid
        BidDetails lastOpponentBid = oppBidHistory.getLastBidDetails();
        if (acceptableBids.contains(lastOpponentBid.getBid())
            || lastOpponentBid.getMyUndiscountedUtil() >= acceptanceThreshold
            || this.offeringStrategy.getNextBid().getMyUndiscountedUtil() <= lastOpponentBid.getMyUndiscountedUtil()) {

            return Actions.Accept;
        }

        // if the average utility of the last bids is twice as small as the opponents bid
        // then it might be the case that we found a "good bid"
        // and thus if it is above a certain threshold then we accept.
        List<BidDetails> nLastBids;
        int oppHistorySize = oppBidHistory.size();
        if (allOppBids.size() < params.get(OPP_BIDS_AMOUNT).intValue()){
            nLastBids = allOppBids.subList(0, oppHistorySize);
        }
        else{
            nLastBids = allOppBids.subList(oppHistorySize - params.get(OPP_BIDS_AMOUNT).intValue(), oppHistorySize);
        }

        double totalUtil = 0;
        for (BidDetails nLastBid : nLastBids) {
            totalUtil += nLastBid.getMyUndiscountedUtil();
        }
        double avgTotalUtil = totalUtil / nLastBids.size();

        // it could be the case that the opponents makes bids that are a little below the acceptance utility and are an outlier compared
        // to the last previous bids, so it could be beneficial to accept the offer. (it perhaps is an onetime-offer)
        if (avgTotalUtil * 2 < lastOpponentBid.getMyUndiscountedUtil()
                && lastOpponentBid.getMyUndiscountedUtil()> (acceptanceThreshold * 0.9)
                && nLastBids.size() >= params.get(OPP_BIDS_AMOUNT).intValue() ) {
            return Actions.Accept;
        }

        // if no acceptance conditions where met until now, reject offer
        return Actions.Reject;
    }
}