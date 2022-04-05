package src.mas2022.group7;

import agents.bayesianopponentmodel.BayesianOpponentModel;
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
    public String getName() {
        return "2022 - Agent007 Bidding Strategy";
    }

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
        BidDetails bid = orderedBids.get(0);

        // phase 1
        List<BidDetails> greatBids = outcomeSpace.getBidsinRange(new Range(0.90, 1.00));
        // todo: use divergence distance (smaller than 0.1 and 20% of the time has passed) to pass the phase
        if (timeline.getTime() / timeline.getTotalTime() * 100 <= 0.50) {
            int randomNum = ThreadLocalRandom.current().nextInt(0, greatBids.size());
            bid = greatBids.get(randomNum);
            return bid;
        }

        // phase 2
        // filtering good bids that are spaced out on their preferences (high utility but different preferences)
        if (!bidsWereFiltered) {
            bidsWereFiltered = true;
            spacedBids.add(orderedBids.get(0));
            boolean isDistant = true;

            for (BidDetails bd : orderedBids) {
                for (int j = 0; j <= spacedBids.size()-1; j++) {
                    double dist = spacedBids.get(j).getBid().getDistance(bd.getBid());
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
        }

        // phase 3 - exploratory phase
        // looking at all the spaced out bids and seeing what matches best with our opponent model
        if (bidsWereFiltered) {
            double bestBid = -999;
            // choose starting point based on opponent model
            // attempt to find the most beneficial agreement for both parties
            BidDetails lastBid = negotiationSession.getOpponentBidHistory().getFirstBidDetails();
            for (BidDetails bd: spacedBids) {
                double evaluatingBid = this.opponentModel.getBidEvaluation(lastBid.getBid());
                // System.out.println(evaluatingBid);
                // System.out.println(bd);
                if (evaluatingBid > bestBid) {
                    bestBid = evaluatingBid;
                    bid = bd;
                }
            }
        }

        return bid;
    }
}