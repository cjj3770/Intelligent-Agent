import java.util.List;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.Domain;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;

/**
 * ExampleAgent returns the bid that maximizes its own utility for half of the negotiation session.
 * In the second half, it offers a random bid. It only accepts the bid on the table in this phase,
 * if the utility of the bid is higher than Example Agent's last bid.
 */
public class ExampleAgent extends AbstractNegotiationParty {
    private final String description = "Example Agent";

    private Bid lastReceivedOffer; // offer on the table
    private Bid myLastOffer;

    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
        AbstractUtilitySpace utilitySpace = estimateUtilitySpace_a10();
        AdditiveUtilitySpace additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;

        List< Issue > issues = additiveUtilitySpace.getDomain().getIssues();

        for (Issue issue : issues) {
            int issueNumber = issue.getNumber();
            System.out.println(">> " + issue.getName() + " weight: " + additiveUtilitySpace.getWeight(issueNumber));

            // Assuming that issues are discrete only
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
            EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) additiveUtilitySpace.getEvaluator(issueNumber);

            for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
                System.out.println(valueDiscrete.getValue());
                System.out.println("Evaluation(getValue): " + evaluatorDiscrete.getValue(valueDiscrete));
                try {
                    System.out.println("Evaluation(getEvaluation): " + evaluatorDiscrete.getEvaluation(valueDiscrete));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

//        List<Bid> bids = userModel.getBidRanking().getBidOrder();
//        for (Bid bid : bids) {
//            List<Issue> issuesList = bid.getIssues();
//            for (Issue issue : issuesList){
//                System.out.println(issue.getName()+ ": " + bid.getValue(issue.getNumber()));
//            }
//        }


    }

    /**
     * When this function is called, it is expected that the Party chooses one of the actions from the possible
     * action list and returns an instance of the chosen action.
     *
     * @param list
     * @return
     */
    @Override
    public Action chooseAction(List<Class<? extends Action>> list) {
        // According to Stacked Alternating Offers Protocol list includes
        // Accept, Offer and EndNegotiation actions only.
        double time = getTimeLine().getTime(); // Gets the time, running from t = 0 (start) to t = 1 (deadline).
        // The time is normalized, so agents need not be
        // concerned with the actual internal clock.


        // First half of the negotiation offering the max utility (the best agreement possible) for Example Agent
        if (time < 0.5) {
            return new Offer(this.getPartyId(), this.getMaxUtilityBid());
        } else {

            // Accepts the bid on the table in this phase,
            // if the utility of the bid is higher than Example Agent's last bid.
            if (lastReceivedOffer != null
                    && myLastOffer != null
                    && this.utilitySpace.getUtility(lastReceivedOffer) > this.utilitySpace.getUtility(myLastOffer)
                    &&this.utilitySpace.getUtility(lastReceivedOffer)>0.7) {

                return new Accept(this.getPartyId(), lastReceivedOffer);
            } else {
                // Offering a random bid with utility >0.7
                //  myLastOffer = generateRandomBidWithUtility(0.7);

                // Offering a random offer
                myLastOffer = generateRandomBidWithUtility(0.7);
                return new Offer(this.getPartyId(), myLastOffer);
            }
        }
    }

    /**
     * This method is called to inform the party that another NegotiationParty chose an Action.
     * @param sender
     * @param act
     */
    @Override
    public void receiveMessage(AgentID sender, Action act) {
        super.receiveMessage(sender, act);

        if (act instanceof Offer) { // sender is making an offer
            Offer offer = (Offer) act;

            // storing last received offer
            lastReceivedOffer = offer.getBid();
        }
    }

    /**
     * A human-readable description for this party.
     * @return
     */
    @Override
    public String getDescription() {
        return description;
    }

    private Bid getMaxUtilityBid() {
        try {
            return this.utilitySpace.getMaxUtilityBid();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public Bid generateRandomBidWithUtility(double utilityThreshold) {
        Bid randomBid;
        double utility;
        do {
            randomBid = generateRandomBid();
            try {
                utility = utilitySpace.getUtility(randomBid);
            } catch (Exception e)
            {
                utility = 0.0;
            }
        }
        while (utility < utilityThreshold);
        return randomBid;
    }
    public AbstractUtilitySpace estimateUtilitySpace_a10() {
        //定义一个double型的二维数组，用来存放K值
        //Mun[m][n],m是issue的个数，n是value的个数
        double Num[][];
        //获取进来的domain
        Domain domain = getDomain();
        //根据domain生成factory
        AdditiveUtilitySpaceFactory factory = new AdditiveUtilitySpaceFactory(domain);
        //usermodel生成一个BidRanking，里面都是一个个bid,放到r里
        BidRanking r = userModel.getBidRanking();
        //计算一共有多少个bid,赋值给totalnum
        int totalnum = r.getBidOrder().size();
        System.out.println("totalnum:"+totalnum);
        double points = 0;
        for (Bid b : r.getBidOrder()) {

            List<Issue> issues = b.getIssues();
            int m=issues.size();
//            List<Issue> issuesList = bid.getIssues();
            //           System.out.println("this is Bid "+b.getValue(1)+" "+b.getValue(2)+" "+b.getValue(3));
            for (Issue issue : issues) {
                //int n=issue
                System.out.println(issue.getName() + ": " + b.getValue(issue.getNumber()));
                System.out.println(issue.getNumber()+"*****************************");
            }

            /////////////////////////////////////////
            for (Issue i : issues) {
                int no = i.getNumber();
                ValueDiscrete v = (ValueDiscrete) b.getValue(no);
                double oldUtil = factory.getUtility(i, v);
//                System.out.println("old utility of "+i.getName()+i.getNumber()+": "+oldUtil);
                factory.setUtility(i, v, oldUtil + points);
            }
            points += 1;
        }
        factory.normalizeWeightsByMaxValues();

        return factory.getUtilitySpace();
//        return new AdditiveUtilitySpaceFactory(getDomain()).getUtilitySpace();

    }


}

