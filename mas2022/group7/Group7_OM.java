import agents.anac.y2018.agreeableagent2018.FrequencyBasedOpponentModel;
import agents.bayesianopponentmodel.BayesianOpponentModel;
import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.OpponentModel;
import genius.core.BidHistory;
import genius.core.list.Tuple;
import genius.core.utility.AdditiveUtilitySpace;
import negotiator.boaframework.opponentmodel.IAMhagglerBayesianModel;

import javax.swing.*;
import java.util.List;


import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import agents.bayesianopponentmodel.BayesianOpponentModel;
import agents.bayesianopponentmodel.OpponentModelUtilSpace;
import genius.core.Bid;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.Issue;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.AdditiveUtilitySpace;


public class Group7_OM extends OpponentModel {

    // This code was gotten from Bayesianopponentmodel.class

    /**
     * Adapter for BayesianModel. Note that this model only works on small domains.
     *
     * Adapted by Mark Hendrikx to be compatible with the BOA framework.
     *
     * Tim Baarslag, Koen Hindriks, Mark Hendrikx, Alex Dirkzwager and Catholijn M.
     * Jonker. Decoupling Negotiating Agents to Explore the Space of Negotiation
     * Strategies
     *
     * @author Mark Hendrikx
     */


        /** Reference to the normal Bayesian Opponent Model */
        private BayesianOpponentModel model;
        /** Index of the first issue weight */
        private int startingBidIssue = 0;

        /**
         * Initializes the opponent model. If the parameter m is set to a value
         * greater than zero, only the best hypothesis about the opponent's utility
         * space is used.
         */
        @Override
        public void init(NegotiationSession negotiationSession, Map<String, Double> parameters) {
            this.negotiationSession = negotiationSession;
            model = new BayesianOpponentModel((AdditiveUtilitySpace) negotiationSession.getUtilitySpace());
            if (parameters.get("m") != null) {
                model.setMostProbableUSHypsOnly(parameters.get("m") > 0);
            } else {
                model.setMostProbableUSHypsOnly(false);
                System.out.println("Constant \"m\" was not set. Assumed default value.");
            }
            while (!testIndexOfFirstIssue(negotiationSession.getUtilitySpace().getDomain().getRandomBid(null),
                    startingBidIssue)) {
                startingBidIssue++;
            }
        }

        /**
         * Just an auxiliar funtion to calculate the index where issues start on a
         * bid because we found out that it depends on the domain.
         *
         * @return true when the received index is the proper index
         */
        private boolean testIndexOfFirstIssue(Bid bid, int i) {
            try {
                @SuppressWarnings("unused")
                ValueDiscrete valueOfIssue = (ValueDiscrete) bid.getValue(i);
            } catch (Exception e) {
                return false;
            }
            return true;
        }

        /**
         * Update the opponent model by updating all hypotheses about the opponent's
         * preference profile.
         *
         * @param opponentBid
         * @param time
         *            of offering
         */
        @Override
        public void updateModel(Bid opponentBid, double time) {
            try {
                double maxTime = 200;
                double timeStep = 0.5;
                int i = 0;
                if(time > 0){
                    while (i <= maxTime && time <= maxTime){
                        model.updateBeliefs(opponentBid);
                        maxTime -= timeStep;
                        i++;
                        timeStep++;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public double getBidEvaluation(Bid bid) {
            try {
                return model.getNormalizedUtility(bid);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

        /**
         * @return estimated issue weight of the given issue.
         */
        public double getWeight(Issue issue) {
            return model.getNormalizedWeight(issue, startingBidIssue);
        }

        /**
         * @return utilityspace created by using the opponent model adapter.
         */
        @Override
        public AdditiveUtilitySpace getOpponentUtilitySpace() {
            return new OpponentModelUtilSpace(model);
        }

        public void cleanUp() {
            super.cleanUp();
        }



        @Override
        public Set<BOAparameter> getParameterSpec() {
            Set<BOAparameter> set = new HashSet<BOAparameter>();
            set.add(new BOAparameter("m", 0.0, "If higher than 0 the most probable hypothesis is only used"));
            return set;
        }

        @Override
        public String getName() {
            return "opponent model test2";
    }

}
