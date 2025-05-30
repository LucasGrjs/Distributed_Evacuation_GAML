package gama.experimental.argumentation.skills;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gama.annotations.precompiler.GamlAnnotations.action;
import gama.annotations.precompiler.GamlAnnotations.arg;
import gama.annotations.precompiler.GamlAnnotations.doc;
import gama.annotations.precompiler.GamlAnnotations.example;
import gama.annotations.precompiler.GamlAnnotations.getter;
import gama.annotations.precompiler.GamlAnnotations.setter;
import gama.annotations.precompiler.GamlAnnotations.skill;
import gama.annotations.precompiler.GamlAnnotations.variable;
import gama.annotations.precompiler.GamlAnnotations.vars;
import gama.core.metamodel.agent.IAgent;
import gama.core.runtime.IScope;
import gama.core.runtime.exceptions.GamaRuntimeException;
import gama.core.util.GamaListFactory;
import gama.core.util.GamaMap;
import gama.core.util.GamaMapFactory;
import gama.core.util.GamaPair;
import gama.core.util.IList;
import gama.core.util.IMap;
import gama.core.util.graph.GamaGraph;
import gama.core.util.graph.IGraph;
import gama.experimental.argumentation.types.GamaArgument;
import gama.experimental.argumentation.types.GamaArgumentType;
import gama.gaml.descriptions.ConstantExpressionDescription;
import gama.gaml.operators.Random;
import gama.gaml.skills.Skill;
import gama.gaml.species.ISpecies;
import gama.gaml.statements.Arguments;
import gama.gaml.statements.IStatement;
import gama.gaml.types.IType;
import gama.gaml.types.Types;
import net.sf.jargsemsat.jargsemsat.datastructures.DungAF;

@skill(name = "argumenting")
@vars({ @variable(name = "argumentation_graph", type = IType.GRAPH, init="directed(graph([]))"),

	@variable(name = "source_type_confidence", type = IType.MAP),
		@variable(name = "crit_importance", type = IType.MAP) })
public class ArgumentingSkill extends Skill {
	static public final String ARGUMENTATION_GRAPH = "argumentation_graph";
	static public final String CRIT_IMPORTANCE = "crit_importance";
	static public final String SOURCE_TYPE_CONFIDENCE = "source_type_confidence";

	public static int BLANK = 0;
	public static int IN = 1;
	public static int OUT = 2;
	public static int MUST_OUT = 3;
	public static int UNDEC = 4;

	@getter(ARGUMENTATION_GRAPH)
	static public GamaGraph<GamaArgument, ?> getArgGraph(final IAgent agent) {
		return (GamaGraph<GamaArgument, ?>) agent.getAttribute(ARGUMENTATION_GRAPH);
	}

	@setter(ARGUMENTATION_GRAPH)
	static public void setArgGraph(final IAgent agent, final IGraph s) {
		agent.setAttribute(ARGUMENTATION_GRAPH, s);
	}

	@getter(CRIT_IMPORTANCE)
	static public GamaMap getCritImp(final IAgent agent) {
		return (GamaMap) agent.getAttribute(CRIT_IMPORTANCE);
	}

	@setter(CRIT_IMPORTANCE)
	static public void setCritImpo(final IAgent agent, final GamaMap s) {
		agent.setAttribute(CRIT_IMPORTANCE, s);
	}

	@getter(SOURCE_TYPE_CONFIDENCE)
	static public GamaMap getSourceConf(final IAgent agent) {
		return (GamaMap) agent.getAttribute(SOURCE_TYPE_CONFIDENCE);
	}

	@setter(SOURCE_TYPE_CONFIDENCE)
	static public void setSourceConf(final IAgent agent, final GamaMap s) {
		agent.setAttribute(SOURCE_TYPE_CONFIDENCE, s);
	}

	
	@action(name = "evaluate_argument", args = {
	@arg(name = "argument", type = GamaArgumentType.id, optional = false, doc = @doc("the argument to evaluate")) }, doc = @doc(value = "evaluate the strength  of an argument for the agent", returns = "the strength of argument", examples = {
	@example("float val <- evaluate_argument(one_argument);") }))
	public Double primEvaluateArg(final IScope scope) throws GamaRuntimeException {
		final GamaArgument a = scope.hasArg("argument") ? (GamaArgument) scope.getArg("argument", GamaArgumentType.id): null;
		return evaluate_arg(scope,a);
	}

	private DungAF toDungAF(IGraph graph) {
		List<String> args = new ArrayList<>();
		for (Object v : graph.getVertices()) {
			args.add(((GamaArgument) v).getId());
		}
		List<String[]> attacks = new ArrayList<>();
		for (Object e : graph.getEdges()) {
			String[] pair = new String[2];
			pair[0] = ((GamaArgument) graph.getEdgeSource(e)).getId();
			pair[1] = ((GamaArgument) graph.getEdgeTarget(e)).getId();
			attacks.add(pair);
		}
		return new DungAF(args, attacks);
	}

	private Map<String, GamaArgument> argumentPerName(IGraph graph) {
		Map<String, GamaArgument> argName = new Hashtable<>();
		for (Object v : graph.getVertices()) {
			argName.put(((GamaArgument) v).getId(), (GamaArgument) v);
		}
		return argName;
	}

	private IList<IList> toGAMAList(IGraph graph, HashSet<HashSet<String>> extension) {
		Map<String, GamaArgument> argName = argumentPerName(graph);
		IList<IList> result = GamaListFactory.create();
		for (HashSet<String> ext : extension) {
			IList ags = GamaListFactory.create();
			for (String a : ext) {
				ags.add(argName.get(a));
			}
			result.add(ags);
		}
		return result;
	}

	@action(name = "preferred_extensions", args = {
	@arg(name = "graph", type = IType.GRAPH, optional = true, doc = @doc("the graph to evaluate")) }, doc = @doc(value = "evaluate the preferred extensions of an argument graph", returns = "a list of list of arguments representing the preferred extensions", examples = {
	@example("list<list<argument>> results <- preferred_extensions(a_graph);") }))
	public IList<IList> primComputePreferedExtension(final IScope scope) throws GamaRuntimeException {
		IGraph graph = (IGraph) scope.getArg("graph", IType.GRAPH);
		if (graph == null)
			graph = getArgGraph(scope.getAgent());
		DungAF dungAF = toDungAF(graph);
		return toGAMAList(graph, dungAF.getPreferredExts());
	}

	@action(name = "complete_extensions", args = {
	@arg(name = "graph", type = IType.GRAPH, optional = true, doc = @doc("the graph to evaluate")) }, doc = @doc(value = "evaluate the complete extensions of an argument graph", returns = "a list of list of arguments representing the complete extensions", examples = {
	@example("list<list<argument>> results <-complete_extensions(a_graph);") }))
	public IList<IList> primComputeCompletedExtension(final IScope scope) throws GamaRuntimeException {
		IGraph graph = (IGraph) scope.getArg("graph", IType.GRAPH);
		if (graph == null)
			graph = getArgGraph(scope.getAgent());
		DungAF dungAF = toDungAF(graph);
		return toGAMAList(graph, dungAF.getCompleteExts());
	}

	@action(name = "stable_extensions", args = {
	@arg(name = "graph", type = IType.GRAPH, optional = true, doc = @doc("the graph to evaluate")) }, doc = @doc(value = "evaluate the stable extensions of an argument graph", returns = "a list of list of arguments representing the stable extensions", examples = {
	@example("list<list<argument>> results <-stable_extensions(a_graph);") }))
	public IList<IList> primComputeStableExtension(final IScope scope) throws GamaRuntimeException {
		IGraph graph = (IGraph) scope.getArg("graph", IType.GRAPH);
		if (graph == null)
			graph = getArgGraph(scope.getAgent());
		DungAF dungAF = toDungAF(graph);
		return toGAMAList(graph, dungAF.getStableExts());
	}

	@action(name = "add_attack",args = {
	@arg(name = "graph", type = IType.GRAPH, optional = true, doc = @doc("the graph to which add an attack")),
	@arg(name = "source", type = GamaArgumentType.id, optional = false, doc = @doc("the source (argument) of the attack")),
	@arg(name = "target", type = GamaArgumentType.id, optional = false, doc = @doc("the target (argument) of the attack"))}, 
		doc = @doc(value = "add an attack to the argumention graph", examples = {
	@example("bool is_added <-add_attack(source_arg, target_arg);") }))
	public boolean primAddAttacks(final IScope scope ) throws GamaRuntimeException {
		IGraph graph = (IGraph) scope.getArg("graph", IType.GRAPH);
		if (graph == null)
			graph = getArgGraph(scope.getAgent());
		final GamaArgument source = scope.hasArg("source") ? (GamaArgument) scope.getArg("source", GamaArgumentType.id): null;
		final GamaArgument target = scope.hasArg("target") ? (GamaArgument) scope.getArg("target", GamaArgumentType.id): null;
		
		if ((graph != null) && (source != null) && (graph.containsVertex(source)) && (target!= null) && (graph.containsVertex(target))) {
			Object edge = graph.addEdge(source, target);
			IAgent ag = scope.getAgent();
			final ISpecies context = ag.getSpecies();
			final IStatement.WithArgs evalArgAct = context.getAction("evaluate_argument");
			final Arguments argsTNR = new Arguments();
			argsTNR.put("argument", ConstantExpressionDescription.create(source));
			evalArgAct.setRuntimeArgs(scope, argsTNR);
			Double weight = (Double) evalArgAct.executeOn(scope);
			graph.setEdgeWeight(edge, weight);
			return true;
		} 
		return false;
	}
	
	
	@action(name = "semi_stable_extensions", args = {
	@arg(name = "graph", type = IType.GRAPH, optional = true, doc = @doc("the graph to evaluate")) }, doc = @doc(value = "evaluate the semi stable extensions of an argument graph", returns = "a list of list of arguments representing the semi stable extensions", examples = {
	@example("list<list<argument>> results <-semi_stable_extensions(a_graph);") }))
	public IList<IList> primComputeSemiStableExtension(final IScope scope) throws GamaRuntimeException {
		IGraph graph = (IGraph) scope.getArg("graph", IType.GRAPH);
		if (graph == null)
			graph = getArgGraph(scope.getAgent());
		DungAF dungAF = toDungAF(graph);
		return toGAMAList(graph, dungAF.getSemiStableExts());
	}

	@action(name = "extensions", args = {
	@arg(name = "graph", type = IType.GRAPH, optional = true, doc = @doc("the graph to evaluate")) },
		doc = @doc(value = "evaluate the extensions of an argument graph", returns = "a list of list of arguments representing the extensions", examples = {
	@example("list<list<argument>> results <- extensions();") }))
	public IList<IList> primComputeExtension(final IScope scope) throws GamaRuntimeException {
		return primComputePreferedExtension(scope);
	}

	@action(name = "update_graph", doc = @doc(value = "update the weight of the argumentation graph", examples = {
	@example("do update_graph;") }))
	public void primUpdateArgumentationGraph(final IScope scope) throws GamaRuntimeException {
		IGraph graph = getArgGraph(scope.getAgent());
		final ISpecies context = scope.getAgent().getSpecies();
		final IStatement.WithArgs evalArgAct = context.getAction("evaluate_argument");
		final Arguments argsTNR = new Arguments();

		for (Object v : graph.getVertices()) {
			argsTNR.put("argument", ConstantExpressionDescription.create(v));
			evalArgAct.setRuntimeArgs(scope, argsTNR);
			Double val = (Double) evalArgAct.executeOn(scope);
			Set edges = graph.outgoingEdgesOf(v);
			for (Object e : edges) {
				graph.setEdgeWeight(e, val);
			}
		}
	}

	@action(name = "simplify_graph", doc = @doc(value = "simplify the argumentation graph", examples = {
			@example("do simplify_graph;") }))
	public IGraph primSimplifyArgumentationGraph(final IScope scope) throws GamaRuntimeException {
		IGraph graph = (IGraph) getArgGraph(scope.getAgent()).copy(scope);

		IList edges = (IList) graph.getEdges().copy(scope);
		for (Object e1 : edges) {
			if (graph.containsEdge(e1)) {
				Object s = graph.getEdgeSource(e1);
				Object t = graph.getEdgeTarget(e1);
				Object e2 = graph.getEdge(t, s);
				if (e2 != null) {
					Double w1 = graph.getWeightOf(e1);
					Double w2 = graph.getWeightOf(e2);
					if (w1 > w2)
						graph.removeEdge(e2);
					else if (w2 < w1)
						graph.removeEdge(e1);
				}
			}
		}

		return graph;
	}

	@action(name = "evaluate_conclusion", args = {
	@arg(name = "arguments", type = IType.CONTAINER, optional = false, doc = @doc("the list of arguments to evaluate")) }, doc = @doc(value = "evaluate the conclusion that can be taken from a list of arguments", returns = "the conclusion of the list of arguments", examples = {
	@example("float val <- evaluate_conclusion(args);") }))
	public Double primEvaluateConcl(final IScope scope) throws GamaRuntimeException {
		final IList args = scope.hasArg("arguments") ? (IList) scope.getArg("arguments", IType.LIST) : null;
		if (args == null || args.isEmpty())
			return 0.0;
		double val = 0;
		IAgent ag = scope.getAgent();
		final ISpecies context = ag.getSpecies();
		final IStatement.WithArgs evalArgAct = context.getAction("evaluate_argument");
		final Arguments argsTNR = new Arguments();
		double sum = 0;
		for (Object obj : args) {
			GamaArgument arg = (GamaArgument) obj;
			argsTNR.put("argument", ConstantExpressionDescription.create(arg));
			evalArgAct.setRuntimeArgs(scope, argsTNR);
			Double w = (Double) evalArgAct.executeOn(scope);
			sum += w;
			val += ((arg.getConclusion().equals("+")) ? 1.0 : ((arg.getConclusion().equals("-")) ? -1.0 : 0.0)) * w;
		}
		return sum == 0.0 ? 0.0 : (val / sum);
	}
	
	
	
	

	@action(name = "add_argument",
			args = {@arg(name = "agent", type = IType.AGENT, optional = true, doc = @doc("the agent of which to compute the attitude")),
					@arg(name = "argument", type = GamaArgumentType.id, optional = false, doc = @doc("the argument to add")),
					@arg(name = "graph", type = IType.GRAPH, optional = false, doc = @doc("the global argumentation graph with all the arguments and attacks")) }, doc = @doc(value = "add an argument and all the attacks to the agent argumentation graph", examples = {
							@example("do add_argument(new_agrument, reference_graph);") }))
	public boolean primAddArguments(final IScope scope) throws GamaRuntimeException {
		IAgent agent = scope.hasArg("agent") ? (IAgent) scope.getArg("agent", IType.AGENT) : null;
		if (agent == null) agent = scope.getAgent();
		IGraph<GamaArgument, Object> graph = (IGraph<GamaArgument, Object>) getArgGraph(agent);
		final IGraph<GamaArgument, Object> refGraph = scope.hasArg("graph") ? (IGraph) scope.getArg("graph", IType.GRAPH) : null;
		final GamaArgument initial_arg = scope.hasArg("argument") ? (GamaArgument) scope.getArg("argument", GamaArgumentType.id) : null;
		GamaArgument arg = (GamaArgument) initial_arg.copy(scope);
		if ((graph != null) && (initial_arg != null) && !(graph.containsVertex(arg))) {
			graph.addVertex(arg);
			if (refGraph != null) {
				Set edges = (Set) refGraph.outgoingEdgesOf(arg);
				for (Object e : edges) {
					GamaArgument t = refGraph.getEdgeTarget(e);
					if (graph.containsVertex(t))
						graph.addEdge(arg, t);
				}
				edges = refGraph.incomingEdgesOf(arg);
				for (Object e : edges) {
					GamaArgument s = refGraph.getEdgeSource(e);
					if (graph.containsVertex(s))
						graph.addEdge(s, arg);
				}
			}
			return true;
		}
		return false;
	}

	@action(name = "remove_argument",
			args = { @arg(name = "argument", type = GamaArgumentType.id, optional = false, doc = @doc("the argument to remove")) }, doc = @doc(value = "remove and arguments and all the attacks concerning this argument from the agent argumentation graph", examples = {
					@example("do remove_argument(an_agrument);") }))
	public static boolean primRemoveArguments(final IScope scope) throws GamaRuntimeException {
		IGraph graph = getArgGraph(scope.getAgent());
		final GamaArgument arg = scope.hasArg("argument") ? (GamaArgument) scope.getArg("argument", GamaArgumentType.id)
				: null;
		if ((graph != null) && (arg != null) && (graph.getVertices().contains(arg))) {
			graph.removeVertex(arg);

			return true;
		}
		return false;
	}

	@action(name = "make_decision", doc = @doc(value = "make decision concerning the option proposed by the argumentation graph", returns = "a pair best extension:value: if the value is > 0, pro option; if value < 0, cons option, elsewere neutral", examples = {
			@example("do make_decision;") }))
	public GamaPair<IList<GamaArgument>, Double>  primMakeDecision(final IScope scope) throws GamaRuntimeException {
		return ((GamaPair<IList<GamaArgument>, Double>) scope.getAgent().getSpecies().getAction("get_best_extension").executeOn(scope));
	}
	

	@action(name = "get_best_extension")
	public GamaPair<IList<GamaArgument>, Double> primGetBestExtension(final IScope scope) throws GamaRuntimeException {
	
		final IStatement evaluateExtensions = scope.getAgent().getSpecies().getAction("evaluate_extensions");
		IMap<IList<GamaArgument>, Double> extensionsEvaluations = (IMap<IList<GamaArgument>, Double>) evaluateExtensions.executeOn(scope);
		
		Iterator<IList<GamaArgument>> it = Random.opShuffle(scope, extensionsEvaluations.getKeys()).iterator();
		IList<GamaArgument> res = it.hasNext() ? it.next() : null;
		double maxVal = res == null ? 0.0 : Math.abs(extensionsEvaluations.get(res));
		while(it.hasNext()) {
			IList<GamaArgument> ext = it.next();
			double v = Math.abs(extensionsEvaluations.get(ext));
			if (v > maxVal) {
				res = ext;
				maxVal = v;
			}
		}
		return new GamaPair<IList<GamaArgument>, Double>(res,  extensionsEvaluations.get(res), Types.LIST, Types.FLOAT);
	}
	
	@action(name = "evaluate_extensions")
	public IMap<IList<GamaArgument>, Double> primEvaluateExtensions(final IScope scope) throws GamaRuntimeException {
		IAgent ag = scope.getAgent();
		final ISpecies context = ag.getSpecies();
		final IStatement updateGraphArg = context.getAction("update_graph");
		updateGraphArg.executeOn(scope);

		final IStatement simplifyGraph = context.getAction("simplify_graph");
		IGraph simplifiedGraph = (IGraph) simplifyGraph.executeOn(scope);

		final IStatement.WithArgs extensionComputation = context.getAction("extensions");
		final Arguments argsComputeExts = new Arguments();
		argsComputeExts.put("graph", ConstantExpressionDescription.create(simplifiedGraph));
		extensionComputation.setRuntimeArgs(scope, argsComputeExts);
		IList<IList<GamaArgument>> extensions = (IList<IList<GamaArgument>>) extensionComputation.executeOn(scope);
		
		IMap<IList<GamaArgument>, Double> resultMap = GamaMapFactory.create(Types.LIST, Types.FLOAT);
		
		final IStatement.WithArgs valExtension = context.getAction("evaluate_conclusion");
		final Arguments argsComputeValExtension = new Arguments();
		IList args = extensions.get(0);
		for (IList ext : extensions) {
			argsComputeValExtension.put("arguments", ConstantExpressionDescription.create(ext));
			valExtension.setRuntimeArgs(scope, argsComputeValExtension);
			resultMap.addValueAtIndex(scope, ext, (Double) valExtension.executeOn(scope));
		}
		return resultMap;
	}
	
	@action(name = "get_arguments_acceptabilities", 
			args = {@arg(name = "agent", type = IType.AGENT, optional = true, doc = @doc("the agent of which to compute the arguments acceptabilities")),
					@arg(name = "semantics", type = IType.STRING, optional = true, doc = @doc("the type of semantics used to compute the arguments acceptabilities: 'max', 'card', 'categorizer'"))},
			doc = @doc(value = "compute acceptability values for all known arguments using the Amgoud and al. acceptability semantics for weighted argumentation graphs", returns = "a map with arguments as key and acceptability as their respectives values", examples = {
			@example("map<argument,float> acceptability_values <- get_arg_acceptability();") }))
	public IMap<GamaArgument, Double> primGetArgumentsAcceptabilities(final IScope scope) throws GamaRuntimeException {
		IAgent agent = scope.hasArg("agent") ? (IAgent) scope.getArg("agent", IType.AGENT) : null;
		if (agent == null) agent = scope.getAgent();
		String semantics = scope.hasArg("semantics") ? scope.getStringArg("semantics") : "max";
		IGraph<GamaArgument, Object> graph = (IGraph<GamaArgument, Object>) getArgGraph(agent);
		
		IMap<GamaArgument,Double> basic_strength = GamaMapFactory.create();
		IMap<GamaArgument,Double> acceptability_values = GamaMapFactory.create();
		
		for (GamaArgument v : graph.getVertices()) {
			basic_strength.addValueAtIndex(scope, v, evaluate_arg(scope, v));
			acceptability_values.addValueAtIndex(scope,  v, evaluate_arg(scope, v));
		}
		double threshold_acceptability_delta = 0.001f;
		double acceptability_delta = 1f;
		Set<Object> incoming_edges ;
		boolean maxSemantics = false;
		boolean cardSemantics = false;
		if ("card".equals(semantics)) {
			cardSemantics = true;
		} else if ("max".equals(semantics)) {
			maxSemantics = true;
		} 
		while (acceptability_delta > threshold_acceptability_delta) {
			acceptability_delta = 0f;
			for (GamaArgument v : graph.getVertices()) {
				double attacking_acceptability = 0;
				incoming_edges = graph.incomingEdgesOf(v);
				if (incoming_edges.size()>0) { 
					int numEdges = incoming_edges.size();
					for(Object e : incoming_edges){
						double attAccep = acceptability_values.get(graph.getEdgeSource(e));
						if (maxSemantics) 
							attacking_acceptability = Math.max(attAccep, attacking_acceptability);
						else 
							attacking_acceptability += attAccep;
					}
					if (cardSemantics) 
						attacking_acceptability = numEdges + attacking_acceptability/numEdges;
				}
				double acceptability = basic_strength.get(v) / (1 + attacking_acceptability);
				
				acceptability_delta = Math.max(acceptability_values.get(v) - acceptability, acceptability_delta);
				//The news acceptabilities values are smaller or equal to the previous one so the delta between those 2 values can be computed without using the absolute function
				acceptability_values.addValueAtIndex(scope, v, acceptability);
			}
		}
		return acceptability_values;
	}
		
	// return the strength of a given argument for a specific agent
	private double evaluate_arg(IScope scope, GamaArgument arg) {
		double val = 0;
		IAgent ag = scope.getAgent();
		if ( arg.getCriteria().isEmpty()) {
			val = 1.0;
		} else {
			Map<String, Double> agVal = getCritImp(ag);
			for (String c : arg.getCriteria().keySet()) {
				val += arg.getCriteria().get(c) * (agVal.containsKey(c) ? agVal.get(c) : 1.0);
			}
		}
		
		if (arg.getSourceType() != null && !arg.getSourceType().isBlank()) {
			Map<String,Double> sourceEval = getSourceConf(ag);
			if (sourceEval.containsKey(arg.getSourceType())) {
				val *= sourceEval.get(arg.getSourceType());
			}
		}
		
		return val;
	}
}
