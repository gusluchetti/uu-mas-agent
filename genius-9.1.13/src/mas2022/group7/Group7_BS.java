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

    public BidDetails actualBid;

    public boolean bidsWereFiltered = false;
    public boolean acquiredInfo = false;
    public boolean queueStarted = false;
    public double bestEval;

    public List<BidDetails> spacedBids = new ArrayList<>();
    public List<BidDetails> orderedBids = new ArrayList<>();
    public List<BidDetails> queuedBids = new ArrayList<>();
    public List<Double> euclidianDistances = new ArrayList<>();

    public double avgUtil = 0D;

    public double[] weights;
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
        actualBid = orderedBids.get(0);

        avgUtil = this.getAvgUtilAllBids();

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
        double dif = negotiationSession.getMaxBidinDomain().getMyUndiscountedUtil() - avgUtil;
        double greatThresh = avgUtil + (0.6 * dif);

        // phase 1 - acquiring info about opponent
        do {
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

            // random great bids (great bids are determined based on domain)
            if ((isUnderAvg && timeline.getTime() <= 0.20) || (!isUnderAvg && timeline.getTime() <= 0.60)) {
                List<BidDetails> greatBids = outcomeSpace.getBidsinRange(new Range(greatThresh, 1.00));
                int randomNum = ThreadLocalRandom.current().nextInt(0, greatBids.size());
                actualBid = greatBids.get(randomNum);
                return actualBid;
            } else {
                acquiredInfo = true;
            }
        } while (!acquiredInfo);

        // phase 2
        // filtering good bids that are spaced out on their preferences (high utility but different preferences)
        if (!bidsWereFiltered) {
            bidsWereFiltered = true;
            bestEval = -999;
            boolean isDistant = true;

            for (BidDetails bd : orderedBids) {
                for (BidDetails spacedBid : spacedBids) {
                    double dist = spacedBid.getBid().getDistance(bd.getBid());
                    if (dist < 0.4) {
                        isDistant = false;
                        break;
                    }
                }

                if (isDistant && bd.getMyUndiscountedUtil() > greatThresh) {
                    spacedBids.add(bd);
                }
                isDistant = true;
            }
        }

        // phase 3 - exploratory phase
        // looking at all the spaced out bids and seeing what matches best with our opponent model
        if (bidsWereFiltered) {
            // choose starting point based on opponent model
            queuedBids = spacedBids;
            if (!queueStarted) {
                bestEval = opponentModel.getBidEvaluation(queuedBids.get(0).getBid());
                queueStarted = true;
                return actualBid;
            } else {
                int chosenIndex = 0;
                for (int i=0;i<queuedBids.size()-1;i++) {
                    double eval = opponentModel.getBidEvaluation(queuedBids.get(i).getBid());
                    if (eval >= bestEval) {
                        bestEval = eval;
                        actualBid = queuedBids.get(i);
                        chosenIndex = i;
                        break;
                    }
                }

                if (!queuedBids.isEmpty()) {
                    queuedBids.remove(chosenIndex);
                }
            }
        }

        return actualBid;
    }
}