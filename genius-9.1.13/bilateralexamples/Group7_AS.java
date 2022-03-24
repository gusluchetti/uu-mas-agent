package bilateralexamples;

import genius.core.Bid;
import genius.core.BidIterator;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.AcceptanceStrategy;
import genius.core.boaframework.Actions;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.utility.AbstractUtilitySpace;

import java.util.*;

public class Group7_AS extends AcceptanceStrategy {
    NegotiationSession neg;

    double util = 0.8;
    public Bid lastOffer;
    public AbstractUtilitySpace utilitySpace;
    public List<Bid> acceptableBids = new ArrayList();
    public double accep_util;
    List<BidDetails> nBestBids;
    double tot_util;

    public Set<BOAparameter> getParameterSpec() {
        HashSet parameters1 = new HashSet();
        // these number are chosen in a way that at the end of the session the acceptance rate has fallen to 0.6. From 0.6 time the decline starts happening from 0.9
        parameters1.add(new BOAparameter("abs_thresh", 0.6D, "absolute lowest utility that is acceptable"));
        parameters1.add(new BOAparameter("rate of decline", 0.866D, "Concession rate"));
        parameters1.add(new BOAparameter("time to start declining", 0.6D, "time to start declining"));
        parameters1.add(new BOAparameter("first reservation value", 0.9D, "first reservation value"));
        parameters1.add(new BOAparameter("amount of opponent bids used", 5D, "amount of opponent bids used"));
        return parameters1;}


    @Override
    public Actions determineAcceptability() {
        //Part 1 where the acceptability starts as a line and after a certain time start declining parabolically.
        // (parameters: time to start declining, reservation value at time 0, rate of decline)
        Map<String, Double> par = this.getParameters();
        BidIterator allbids = new BidIterator(this.negotiationSession.getDomain());
        if (this.negotiationSession.getTime() < par.get("time to start declining")){
            accep_util = par.get("first reservation value");
        }
        else {accep_util = par.get("first reservation value") - (this.negotiationSession.getTime() - par.get("time to start declining")) * Math.pow(par.get("rate of decline"), 2); }

        while (allbids.hasNext()) {
            Bid next_bid = allbids.next();
            if (this.negotiationSession.getUtilitySpace().getUtility(next_bid) > accep_util) {
                acceptableBids.add(next_bid);
            }
        }
            BidDetails lastopponentbid = this.negotiationSession.getOpponentBidHistory().getLastBidDetails();
        if (acceptableBids.contains(lastopponentbid.getBid())) {
            return Actions.Accept;
        }

        // part 2, if the opponents bid has a higher utility than our next bid then accept
        if (this.offeringStrategy.getNextBid().getMyUndiscountedUtil() < lastopponentbid.getMyUndiscountedUtil()){
            return Actions.Accept;
        }

        // part 3, if the average utility of the last bids is twice as small as the opponents bid, then it might be the case that we found a "good bid"
        // and thus if it above a certain threshold then we accept.  (parameters: size of history that you want to include)
        int size = this.negotiationSession.getOpponentBidHistory().size();

        if (this.negotiationSession.getOpponentBidHistory().getHistory().size() < par.get("amount of opponent bids used").intValue()){
            nBestBids = this.negotiationSession.getOpponentBidHistory().getHistory().subList(0, size);
        }
        else{nBestBids = this.negotiationSession.getOpponentBidHistory().getHistory().subList(size - par.get("amount of opponent bids used").intValue(), size);}

        tot_util = 0;
        for (int i = 0; i < nBestBids.size();i++){
            tot_util += nBestBids.get(i).getMyUndiscountedUtil();

        }
        double avg_util = tot_util / nBestBids.size();
        //this is used to find bids that happen only once in the negotation session and thus are one of the best that can be accepted
        if (avg_util * 2 < lastopponentbid.getMyUndiscountedUtil() && lastopponentbid.getMyUndiscountedUtil()> par.get("abs_thresh") && nBestBids.size() >= par.get("amount of opponent bids used").intValue() ){
            return Actions.Accept;
        }
        return Actions.Reject;
    }
    public String getName() {
        return "acceptance strategy agent 007 -1";
    }
}
