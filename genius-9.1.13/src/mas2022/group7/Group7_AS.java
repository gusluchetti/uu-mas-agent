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
    public double accep_util;
    List<BidDetails> nLastBids;
    double tot_util;
    List<BidDetails> opponent_bids;
    List<Double> list_with_util = new ArrayList();
    double sum;
    double sum_index;
    double sum_denominator;
    double sum_numerator;
    double slope;
    double intercept;
    double sumofallbidsutil;
    List<BidDetails> all_biddetails;
    boolean isavgcalc;
    double avgofallutil;

    public Set<BOAparameter> getParameterSpec() {
        HashSet parameters1 = new HashSet();
        // From 0.6 time the decline starts happening from 0.95
        parameters1.add(new BOAparameter("time to start declining", 0.6D, "time to start declining"));
        parameters1.add(new BOAparameter("first utility value", 0.95D, "first utility value"));
        parameters1.add(new BOAparameter("amount of opponent bids used", 5D, "amount of opponent bids used"));
        return parameters1;}


    @Override
    public Actions determineAcceptability() {
        sumofallbidsutil = 0;
        sum = 0;
        sum_index = 0;
        opponent_bids = this.negotiationSession.getOpponentBidHistory().getHistory();
        Map<String, Double> par = this.getParameters();
        //make the utility prediction of the opponents bids after certain time is passed so that the line becomes more reliable.
        if (this.negotiationSession.getTime() > par.get("time to start declining")){
            for(int i = 0; i < this.opponent_bids.size(); i++){
                double util = this.opponent_bids.get(i).getMyUndiscountedUtil();
                list_with_util.add(util);
                sum =+ sum + util;
                sum_index =+ sum_index + i;
            }
            //least squares regression to find the best fit of a line through the utilities of the opponent.
            //avg of y coordinates (avg_utility);
            double avg_y = sum / this.opponent_bids.size();
            //avg of x_coordinates is the average of the indexs
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
        (parameters: time to start declining,first util value) */
        BidIterator allbids = new BidIterator(this.negotiationSession.getDomain());

        if (this.negotiationSession.getTime() < par.get("time to start declining")){
            accep_util = par.get("first utility value");
            //the average utility is only needed after the time it start declining. so whether it is calculated is set to false;
            isavgcalc = Boolean.FALSE;

        }
        else {
            BidDetails opponent_bids_best = this.negotiationSession.getOpponentBidHistory().getBestBidDetails();
            // the opponent best bid utility
            double util_best_opp_bid = opponent_bids_best.getMyUndiscountedUtil();
            //approximation of total amount of bids of opponent
            double amount_ofbids = this.opponent_bids.size()/this.negotiationSession.getTime();

            /*the predicted utility of the opponents last bid;
            use line you created in the previous section
            line becomes y = slope * x + intercept*/
            double pred = slope * amount_ofbids + intercept;
            //@@@@
            all_biddetails = this.negotiationSession.getOutcomeSpace().getAllOutcomes();
            //find avg of all util once by using boolean to whether it was calculated;
            if (!isavgcalc) {
                for (int i = 0; i < all_biddetails.size(); i++) {
                    sumofallbidsutil += all_biddetails.get(i).getMyUndiscountedUtil();
                }
                avgofallutil = sumofallbidsutil / all_biddetails.size();
                //now it is calculated
                isavgcalc = Boolean.TRUE;
            }

            // if the prediction of the last point is higher than the utility of the opponents highest bid then use it as an endpoint.
            if (pred > util_best_opp_bid){
                /*pred = accep_util - (timeleft) * decline ^ 2
                 endpoint means the point of lowest acceptability utility at the end of the time*/
                double endpoint = pred;
                //failsafe since you dont want it to go lower than the average of all bids and this depends a lot on the domain;
                if (endpoint < avgofallutil){
                    endpoint = avgofallutil;
                }
                //failsafe if the endpoint is higher than acceptutil then it should go up but instead we will let it stay the same.-
                if (endpoint > accep_util){
                    endpoint = accep_util ;
                }
                // decline = sqrt((pred - accep_util) / - (timeleft)))
                // this decline rate will take care that the accept util will finish at the endpoint at the end of time.
                double rateofdecline = Math.sqrt((endpoint - accep_util) / - (1- this.negotiationSession.getTime()));

                // to find the average timestep between opponent bids
                double timestep = this.negotiationSession.getTime() / this.opponent_bids.size();
                accep_util = accep_util - (timestep * Math.pow(rateofdecline, 2));
            }
            //else use the utility of the util_best_opp_bid
            else{
                double endpoint = util_best_opp_bid;
                //failsafe since you dont want it to go lower than the average of all bids util
                if (endpoint < avgofallutil){
                    endpoint = avgofallutil;
                }
                //failsafe if the endpoint is higher than acceptutil then it should go up but instead we will let it stay the same.
                if (endpoint > accep_util){
                    endpoint = accep_util ;
                }
                // this decline rate will take care that the accept util will finish at the endpoint at the end of time.
                double rateofdecline = Math.sqrt((endpoint - accep_util) / - (1- this.negotiationSession.getTime()));

                // to find the average timestep between opponent bids
                double timestep = this.negotiationSession.getTime() / this.opponent_bids.size();
                accep_util = accep_util - (timestep * Math.pow(rateofdecline, 2));

            }
        }

        while (allbids.hasNext()) {
            Bid next_bid = allbids.next();
            if (this.negotiationSession.getUtilitySpace().getUtility(next_bid) > accep_util) {
                acceptableBids.add(next_bid);
            }
        }

        //check whether opponent bid has a higher utility then the acceptance utility and then accept.
        BidDetails lastopponentbid = this.negotiationSession.getOpponentBidHistory().getLastBidDetails();
        if (acceptableBids.contains(lastopponentbid.getBid())) {
            return Actions.Accept;
        }

        if (lastopponentbid.getMyUndiscountedUtil() >= accep_util){
            return Actions.Accept;

        }
        // part 2, if the opponents bid has a higher or equal utility than our next bid then accept
        if (this.offeringStrategy.getNextBid().getMyUndiscountedUtil() <= lastopponentbid.getMyUndiscountedUtil()){
            return Actions.Accept;
        }

        /*part 3, if the average utility of the last bids is twice as small as the opponents bid, then it might be the case that we found a "good bid"
         and thus if it above a certain threshold then we accept.  (parameters: size of history that you want to include)*/
        int size = this.negotiationSession.getOpponentBidHistory().size();

        if (this.negotiationSession.getOpponentBidHistory().getHistory().size() < par.get("amount of opponent bids used").intValue()){
            nLastBids = this.negotiationSession.getOpponentBidHistory().getHistory().subList(0, size);
        }
        else{nLastBids = this.negotiationSession.getOpponentBidHistory().getHistory().subList(size - par.get("amount of opponent bids used").intValue(), size);}

        tot_util = 0;
        for (int i = 0; i < nLastBids.size();i++){
            tot_util += nLastBids.get(i).getMyUndiscountedUtil();

        }
        double avg_util = tot_util / nLastBids.size();
        /*this is used to find bids that happen only once in the negotation session and thus are one of the best that can be accepted
        it could be the case that the opponents makes bids that are a little below the acceptance utility and are an outlier compared
        to the last previous bids thus it could be beneficial to accept the offer. (it perhaps is an onetime-offer)
         */
        if (avg_util * 2 < lastopponentbid.getMyUndiscountedUtil() && lastopponentbid.getMyUndiscountedUtil()> (accep_util * 0.9)  && nLastBids.size() >= par.get("amount of opponent bids used").intValue() ){
            return Actions.Accept;
        }
        //If neither of these things are satisfied then reject
        return Actions.Reject;
    }


    public String getName() {
        return "acceptance strategy agent 007 -1";
    }
}
