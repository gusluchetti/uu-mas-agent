package src.mas2022.group7;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.*;
import genius.core.Bid;

import genius.core.utility.AbstractUtilitySpace;
import genius.core.BidIterator;

import java.util.*;

public class Group7_AS extends AcceptanceStrategy {
    public Bid lastOffer;
    public AbstractUtilitySpace utilitySpace;
    public List<Bid> acceptableBids = new ArrayList();
    public double accept_util;
    List<BidDetails> nBestBids;
    double tot_util;
    List<BidDetails> opponent_bids;
    List<Double> list_with_util = new ArrayList();

    double sum;
    double sum_index;
    double sum_denominator;
    double sum_numerator;
    double slope;
    double intercept;

    final String ABS_TRESH = "abs_tresh";
    final String DECLINE_START = "time to start declining";
    final String RES_VALUE = "first res value";
    final String OPP_BIDS_AMOUNT = "amount of opponent bids used";

    public Set<BOAparameter> getParameterSpec() {
        Set<BOAparameter> parameters = new HashSet();
        // From 0.6 time the decline starts happening from 0.9
        parameters.add(new BOAparameter(this.ABS_TRESH, 0.8D, "absolute lowest utility that is acceptable"));
        parameters.add(new BOAparameter(this.DECLINE_START, 0.6D, "time to start declining"));
        parameters.add(new BOAparameter(this.RES_VALUE, 0.9D, "first reservation value"));
        parameters.add(new BOAparameter(this.OPP_BIDS_AMOUNT, 5D, "amount of opponent bids used"));
        return parameters;}


    @Override
    public Actions determineAcceptability() {
        sum = 0;
        sum_index = 0;
        opponent_bids = this.negotiationSession.getOpponentBidHistory().getHistory();
        Map<String, Double> par = this.getParameters();
        //make the utility prediction of the opponents bids after certain time is passed so that the line becomes more reliable.
        if (this.negotiationSession.getTime() > par.get(this.DECLINE_START)){
            for(int i = 0; i < this.opponent_bids.size(); i++){
                double util = this.opponent_bids.get(i).getMyUndiscountedUtil();
                list_with_util.add(util);
                sum =+ sum + util;
                sum_index =+ sum_index + i;
            }
            //least squares regression to find the best fit of a line through the utilities of the opponent.
            //avg of y coordinates (avg_utility);
            double avg_y = sum / this.opponent_bids.size();
            //avg of x_coordinates is the average of the indexes
            double avg_x = sum_index / this.opponent_bids.size();
            //slope = sum((x-xa)(y-ya))/ sum((x-xa)(x-xa))
            sum_numerator = 0;
            //x-x_avg * x-x_avg
            sum_denominator =0;
            for (int i = 0; i < this.list_with_util.size(); i++) {
                //x -x_avg * y - y_avg
                //i is in this case x
                sum_numerator = sum_numerator + (i - avg_x) * (list_with_util.get(i) - avg_y);
                //x-x_avg * x-x_avg
                sum_denominator = sum_denominator + (i - avg_x) * (i - avg_x);

            }
            //now we have a slope for our linear fit
            slope = sum_numerator / sum_denominator;
            //get intercept y_avg = slope * x_avg + b

            intercept = avg_y - (slope * avg_x);
            //line becomes y = slope * x + intercept
        }
        /*Part 1 where the acceptability starts as a line and after a certain time start declining parabolically.
        (parameters: time to start declining,first res value) */
        BidIterator allBids = new BidIterator(this.negotiationSession.getDomain());

        if (par.containsKey(this.RES_VALUE) && this.negotiationSession.getTime() < par.get(this.DECLINE_START)){
            accept_util = par.get(this.RES_VALUE);
        }
        else {
            BidDetails opponent_bids_best = this.negotiationSession.getOpponentBidHistory().getBestBidDetails();
            // the opponent best bid utility
            double util_best_opp_bid = opponent_bids_best.getMyUndiscountedUtil();
            //approximation of total amount of bids of opponent
            double amount_of_bids = this.opponent_bids.size()/this.negotiationSession.getTime();

            /*the predicted utility of the opponents last bid;
            use line you created in the previous section
            line becomes y = slope * x + intercept*/
            double pred = slope * amount_of_bids + intercept;

            // if the prediction of the last point is higher than the utility of the opponents highest bid then use it as an endpoint.
            if (pred > util_best_opp_bid){
                /*pred = accep_util - (timeleft) * decline ^ 2
                 endpoint means the point of lowest acceptability utility at the end of the time*/
                double endpoint = pred;
                //failsafe since you dont want it to go lower than 0.6
                if (endpoint < 0.6){
                    endpoint = 0.6;
                }
                // decline = sqrt((pred - accep_util) / - (timeleft)))
                // this decline rate will take care that the accept util will finish at the endpoint at the end of time.
                double rateofdecline = Math.sqrt((endpoint - accept_util) / - (1- this.negotiationSession.getTime()));

                // to find the average timestep between opponent bids
                double timestep = this.negotiationSession.getTime() / this.opponent_bids.size();
                accept_util = accept_util - (timestep * Math.pow(rateofdecline, 2));
            }
            //else use the utility of the util_best_opp_bid
            else{
                double endpoint = util_best_opp_bid;
                //failsafe since you dont want it to go lower than 0.6
                if (endpoint < 0.6){
                    endpoint = 0.6;
                }
                // this decline rate will take care that the accept util will finish at the endpoint at the end of time.
                double rateofdecline = Math.sqrt((endpoint - accept_util) / - (1- this.negotiationSession.getTime()));

                // to find the average timestep between opponent bids
                double timestep = this.negotiationSession.getTime() / this.opponent_bids.size();
                accept_util = accept_util - (timestep * Math.pow(rateofdecline, 2));
            }
        }

        while (allBids.hasNext()) {
            Bid next_bid = allBids.next();
            if (this.negotiationSession.getUtilitySpace().getUtility(next_bid) > accept_util) {
                acceptableBids.add(next_bid);
            }
        }

        //check whether opponent bid has a higher utility then the acceptance utility and then accept.
        BidDetails lastopponentbid = this.negotiationSession.getOpponentBidHistory().getLastBidDetails();
        if (acceptableBids.contains(lastopponentbid.getBid())) {
            return Actions.Accept;
        }

        // part 2, if the opponents bid has a higher or equal utility than our next bid then accept
        if (this.offeringStrategy.getNextBid().getMyUndiscountedUtil() <= lastopponentbid.getMyUndiscountedUtil()){
            return Actions.Accept;
        }

        /*part 3, if the average utility of the last bids is twice as small as the opponents bid, then it might be the case that we found a "good bid"
         and thus if it above a certain threshold then we accept.  (parameters: size of history that you want to include)*/
        int size = this.negotiationSession.getOpponentBidHistory().size();

        if (this.negotiationSession.getOpponentBidHistory().getHistory().size() < par.get(this.OPP_BIDS_AMOUNT).intValue()){
            nBestBids = this.negotiationSession.getOpponentBidHistory().getHistory().subList(0, size);
        }
        else{nBestBids = this.negotiationSession.getOpponentBidHistory().getHistory().subList(size - par.get(this.OPP_BIDS_AMOUNT).intValue(), size);}

        tot_util = 0;
        for (int i = 0; i < nBestBids.size();i++){
            tot_util += nBestBids.get(i).getMyUndiscountedUtil();

        }
        double avg_util = tot_util / nBestBids.size();
        //this is used to find bids that happen only once in the negotation session and thus are one of the best that can be accepted
        if (avg_util * 2 < lastopponentbid.getMyUndiscountedUtil() && lastopponentbid.getMyUndiscountedUtil()> par.get(this.ABS_TRESH) && nBestBids.size() >= par.get(this.OPP_BIDS_AMOUNT).intValue() ){
            return Actions.Accept;
        }
        //If neither of these things are satisfied then reject
        return Actions.Reject;
    }


    public String getName() {
        return "Agent 007 - Acceptance Strategy";
    }
}
