package bilateralexamples.mas2022.group7;

import java.util.Map;

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.*;
import negotiator.boaframework.opponentmodel.DefaultModel;
import negotiator.boaframework.sharedagentstate.anac2011.GahboninhoSAS;
import negotiator.boaframework.sharedagentstate.anac2011.gahboninho.IssueManager;

/*
* Offering strategy mostly based on Gahboninho's Agent bidding strategy (from ANAC 2011)
* */
public class Group7_BS extends OfferingStrategy {
    private boolean WereBidsFiltered = false;
    private int RoundCount = 0;
    private SortedOutcomeSpace OutcomeSpace;

    @Override
    public void init(NegotiationSession domainKnow, OpponentModel model, OMStrategy omStrategy,
                     Map<String, Double> parameters) throws Exception {

        if (model instanceof DefaultModel) {
            model = new NoModel();
        }

        super.init(domainKnow, model, omStrategy, parameters);
        helper = new GahboninhoSAS(negotiationSession);

        if (!(opponentModel instanceof NoModel)) {
            OutcomeSpace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
        }
    }

    @Override
    public BidDetails determineOpeningBid() {
        return determineNextBid();
    }

    @Override
    public BidDetails determineNextBid() {
        IssueManager issueManager = ((GahboninhoSAS) helper).getIssueManager();
        BidDetails previousOpponentBid = null;
        BidDetails opponentBid = negotiationSession.getOpponentBidHistory().getLastBidDetails();

        int histSize = negotiationSession.getOpponentBidHistory().getHistory().size();
        if (histSize >= 2) {
            previousOpponentBid = negotiationSession.getOpponentBidHistory().getHistory().get(histSize - 1);
        }

        double threshold;
        /* if it's not the first turn:
        * process their bid
        * update opponent model
        * learn bids?
        * update minimum utility for acceptance
        * */
        if (opponentBid != null) {
            if (previousOpponentBid != null) {
                try {
                    issueManager.ProcessOpponentBid(opponentBid.getBid());
                    ((GahboninhoSAS) helper).getOpponentModel().UpdateImportance(opponentBid.getBid());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    issueManager.learnBids(opponentBid.getBid());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            threshold = issueManager.GetMinimumUtilityToAccept();
            issueManager.setMinimumUtilForAcceptance(threshold);
        }

        try {
            // on the first few rounds don't get tempted so fast
            ++RoundCount;
            if (!WereBidsFiltered &&
                (negotiationSession.getTime() > issueManager.GetDiscountFactor() * 0.9 ||
                negotiationSession.getTime() + (3 * issueManager.getBidsCreationTime()) > 1)
            ) {
                WereBidsFiltered = true;
                int DesiredBidCount = (int) (RoundCount * (1 - negotiationSession.getTime()));
                if (issueManager.getBids().size() > 200) {
                    issueManager.setBids(
                            ((GahboninhoSAS) helper).getOpponentModel().FilterBids(issueManager.getBids(), DesiredBidCount)
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // on the first time we act offer max bid
        if (previousOpponentBid == null) {
            try {
                issueManager.AddMyBidToStatistics(issueManager.getMaxBid());
            } catch (Exception e) {
                e.printStackTrace();
            }
            Bid maxBid = issueManager.getMaxBid();

            try {
                return new BidDetails(
                        maxBid, negotiationSession.getUtilitySpace().getUtility(maxBid), negotiationSession.getTime()
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Bid myBid;
        if (((GahboninhoSAS) helper).getFirstActions() >= 0 && negotiationSession.getTime() < 0.15) {
            // on first few bids let the opponent learn some more about our preferences
            int totalFirstActions = 40;
            double utilDecrease = (1 - 0.925) / totalFirstActions;

            myBid = issueManager.GenerateBidWithAtleastUtilityOf(0.925 + utilDecrease * ((GahboninhoSAS) helper).getFirstActions());
            ((GahboninhoSAS) helper).decrementFirstActions();
        } else {
            // always execute this one, even when an OM has been set as this method has side-effects.
            myBid = issueManager.GenerateBidWithAtleastUtilityOf(
                    issueManager.GetNextRecommendedOfferUtility());
            if (issueManager.getInFrenzy())
                myBid = issueManager.getBestEverOpponentBid();

        }

        try {
            double utility = negotiationSession.getUtilitySpace().getUtility(myBid);
            if (!(opponentModel instanceof NoModel)) {
                BidDetails selectedBid = omStrategy.getBid(OutcomeSpace, utility);
                issueManager.AddMyBidToStatistics(selectedBid.getBid());
                return selectedBid;
            }
            issueManager.AddMyBidToStatistics(myBid);
            return new BidDetails(myBid, utility, negotiationSession.getTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getName() {
        return "2022 - Agent007";
    }
}