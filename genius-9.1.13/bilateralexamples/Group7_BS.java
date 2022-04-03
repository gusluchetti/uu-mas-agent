package bilateralexamples;

import genius.core.Bid;
import genius.core.BidIterator;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.*;
import genius.core.misc.Range;
import genius.core.timeline.TimeLineInfo;
import genius.core.utility.AbstractUtilitySpace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class Group7_BS extends OfferingStrategy {
    public boolean bidsWereFiltered = false;
    public List<BidDetails> spacedBids = new ArrayList<>();

    @Override
    public void init(NegotiationSession negotiationSession, OpponentModel opponentModel, OMStrategy omStrategy, Map<String, Double> parameters) throws Exception {
        super.init(negotiationSession, opponentModel, omStrategy, parameters);
    }

    @Override
    public BidDetails determineOpeningBid() {
        return negotiationSession.getMaxBidinDomain();
    }

    @Override
    public BidDetails determineNextBid() {
        AbstractUtilitySpace utilitySpace = negotiationSession.getUtilitySpace();
        SortedOutcomeSpace outcomeSpace = new SortedOutcomeSpace(utilitySpace);
        TimeLineInfo timeline = negotiationSession.getTimeline();

        // bid lists
        List<BidDetails> orderedBids = outcomeSpace.getOrderedList();
        BidDetails firstBid = orderedBids.get(0);

        // phase 1
        // if (over 50% time passed && no new information gained from past 3 bids) {
        List<BidDetails> greatBids = outcomeSpace.getBidsinRange(new Range(0.90, 1.00));
        if (timeline.getTime() / timeline.getTotalTime() * 100 <= 0.50) {
            int randomNum = ThreadLocalRandom.current().nextInt(0, greatBids.size());
            firstBid = greatBids.get(randomNum);
            return firstBid;
        }
        // phase 2
        // if (filtered bids have NOT been tested) {
        if (!bidsWereFiltered) {
            bidsWereFiltered = true;
            // to get evenly spaces bids:
            // empty list, add random great bid (above .95)
            spacedBids.add(firstBid);
            boolean isDistant = true;

            for (BidDetails bd : orderedBids) {
                for (int j = 0; j <= spacedBids.size()-1; j++) {
                    double dist = spacedBids.get(j).getBid().getDistance(bd.getBid());
                    // find best bid with distance in actual preferences (at least 0.3)
                    if (dist < 0.4) {
                        isDistant = false;
                        break;
                    }
                }
                if (isDistant && bd.getMyUndiscountedUtil() > 0.60) {
                    spacedBids.add(bd);
                }
                isDistant = true;
            }
            // find another best bid with that distance to all other elements in the list
            // repeat until all bids have been checked
        }
        // phase 3 - exploratory phase
        // if (all filtered bids HAVE been tested) {
        if (bidsWereFiltered) {

        }
        // panic phase - acceptance strategy gets less and less lenient

        int r = ThreadLocalRandom.current().nextInt(0, spacedBids.size());
        return spacedBids.get(r);
    }

    @Override
    public String getName() {
        return "2022 - Agent007";
    }
}