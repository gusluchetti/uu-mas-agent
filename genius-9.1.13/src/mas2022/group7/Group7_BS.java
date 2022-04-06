package src.mas2022.group7;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.*;
import genius.core.issue.Issue;
import genius.core.misc.Range;
import genius.core.timeline.TimeLineInfo;
import genius.core.utility.AbstractUtilitySpace;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Group7_BS extends OfferingStrategy {
    public AbstractUtilitySpace utilitySpace;
    public SortedOutcomeSpace outcomeSpace;
    public TimeLineInfo timeline;
    public List<Issue> issues;

    public boolean bidsWereFiltered = false;
    public double bestEval;
    public List<BidDetails> spacedBids = new ArrayList<>();
    public List<BidDetails> orderedBids = new ArrayList<>();
    public double[] weights;
    public List<Double> euclidianDistances = new ArrayList<>();
    public double totalAvgDistance = 0;

    @Override
    public String getName() {
        return "2022 - Agent007 Bidding Strategy";
    }

    @Override
    public void init(NegotiationSession negotiationSession, OpponentModel opponentModel, OMStrategy omStrategy, Map<String, Double> parameters) throws Exception {
        this.negotiationSession = negotiationSession;
        this.opponentModel = opponentModel;
        this.omStrategy = omStrategy;

        timeline = this.negotiationSession.getTimeline();
        utilitySpace = this.negotiationSession.getUtilitySpace();
        outcomeSpace = new SortedOutcomeSpace(utilitySpace);

        orderedBids = outcomeSpace.getOrderedList();
        spacedBids.add(orderedBids.get(0));
        issues = this.negotiationSession.getIssues();
        weights = new double[issues.size()];

        this.endNegotiation = false;
    }

    @Override
    public BidDetails determineOpeningBid() {
        return negotiationSession.getMaxBidinDomain();
    }

    private double getAvgUtilAllBids() {
        double sumUtility = 0D;
        for (BidDetails bid : orderedBids) {
            sumUtility += bid.getMyUndiscountedUtil();
        }
        return sumUtility / orderedBids.size();
    };

    public double getDistanceBetweenWeights(double[] newWeights) {
        double sumDistances = 0;
        for (int i=0;i < issues.size();i++) {
            sumDistances += Math.pow((weights[i] - newWeights[i]), 2);
        }
        return Math.sqrt(sumDistances);
    };

    public boolean isLastNUnderAvg(int N) {
        boolean check = true;
        int maxIndex = euclidianDistances.size()-1;
        for (int i=maxIndex;i>=maxIndex-N-1;i--) {
            if (euclidianDistances.get(i) > totalAvgDistance) {
                return false;
            }
        }
        return check;
    }

    @Override
    public BidDetails determineNextBid() {
        BidDetails bid = orderedBids.get(0);
        // phase 1 - random great bids (great bids are determined based on domain)
        List<BidDetails> greatBids = outcomeSpace.getBidsinRange(new Range(0.90, 1.00));

        // getting latest opponent weights for each issue
        double[] newWeights = new double[issues.size()];
        for (int i=0;i<issues.size();i++) {
            newWeights[i] = this.opponentModel.getWeight(issues.get(i));
        }

        euclidianDistances.add(getDistanceBetweenWeights(newWeights));
        weights = newWeights.clone();
        double sum = 0D;

        for (double d : euclidianDistances) {
            if (euclidianDistances.indexOf(d) != 0
                && euclidianDistances.indexOf(d) != 1) {
                sum += d;
            }
        }
        totalAvgDistance = sum / euclidianDistances.size();
        boolean isUnderAvg = false;
        if (negotiationSession.getOpponentBidHistory().getHistory().size() > 5) {
            isUnderAvg = isLastNUnderAvg(5);
        }

        if ((isUnderAvg && timeline.getTime() <= 0.20) || (!isUnderAvg && timeline.getTime() <= 0.60)) {
            int randomNum = ThreadLocalRandom.current().nextInt(0, greatBids.size());
            bid = greatBids.get(randomNum);
            return bid;
        }

        // phase 2
        // filtering good bids that are spaced out on their preferences (high utility but different preferences)
        if (!bidsWereFiltered) {
            bestEval = -999;
            bidsWereFiltered = true;

            boolean isDistant = true;
            for (BidDetails bd : orderedBids) {
                for (BidDetails spacedBid : spacedBids) {
                    double dist = spacedBid.getBid().getDistance(bd.getBid());
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
            // choose starting point based on opponent model
            // attempt to find the most beneficial agreement for both parties
            BidDetails lastBid = negotiationSession.getOpponentBidHistory().getLastBidDetails();
            for (BidDetails bd: spacedBids) {
                double lastBidEval = this.opponentModel.getBidEvaluation(bd.getBid());
                if (lastBidEval > bestEval) {
                    bestEval = lastBidEval;
                    bid = bd;
                }
            }
        }

        return bid;
    }
}