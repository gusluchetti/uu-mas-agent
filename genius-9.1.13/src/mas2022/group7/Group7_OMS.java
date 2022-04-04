package src.mas2022.group7;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OMStrategy;

import java.util.*;

// Code taken from BestBid example.
public class Group7_OMS extends OMStrategy {
    /**
     * when to stop updating the opponentmodel. Note that this value is not
     * exactly one as a match sometimes lasts slightly longer.
     */
    double updateThreshold = 1.1;

    /**
     * Initializes the opponent model strategy. If a value for the parameter t
     * is given, then it is set to this value. Otherwise, the default value is
     * used.
     *
     * @param negotiationSession
     *            state of the negotiation.
     * @param parameters
     *            set of parameters for this opponent model strategy.
     */
    @Override
    public void init(NegotiationSession negotiationSession, Map<String, Double> parameters) {
        super.init(negotiationSession, parameters);
        if (parameters.get("t") != null) {
            updateThreshold = parameters.get("t").doubleValue();
        } else {
            System.out.println("OMStrategy assumed t = 1.1");
        }
    }

    /**
     * Returns the best bid for the opponent given a set of similarly preferred
     * bids.
     *
     * @param allBids
     *            of the bids considered for offering.
     * @return bid to be offered to opponent.
     */
    @Override
    public BidDetails getBid(List<BidDetails> allBids) {

        // 1. If there is only a single bid, return this bid
        if (allBids.size() == 1) {
            return allBids.get(0);
        }
        double bestUtil = -1;
        BidDetails bestBid = allBids.get(0);

        // 2. Check that not all bids are assigned at utility of 0
        // to ensure that the opponent model works. If the opponent model
        // does not work, offer a random bid.
        boolean allWereZero = true;
        // 3. Determine the best bid
        for (BidDetails bid : allBids) {
            double evaluation = model.getBidEvaluation(bid.getBid());
            if (evaluation > 0.0001) {
                allWereZero = false;
            }
            if (evaluation > bestUtil) {
                bestBid = bid;
                bestUtil = evaluation;
            }
        }
        // 4. The opponent model did not work, therefore, offer a random bid.
        if (allWereZero) {
            Random r = new Random();
            return allBids.get(r.nextInt(allBids.size()));
        }
        return bestBid;
    }

    /**
     * The opponent model may be updated, unless the time is higher than a given
     * constant.
     *
     * @return true if model may be updated.
     */
    @Override
    public boolean canUpdateOM() {
        return negotiationSession.getTime() < updateThreshold;
    }

    @Override
    public Set<BOAparameter> getParameterSpec() {
        Set<BOAparameter> set = new HashSet<>();
        set.add(new BOAparameter("t", 1.1, "Time after which the OM should not be updated"));
        return set;
    }

    @Override
    public String getName() {
        return "Agent-007OMS";
    }
}
