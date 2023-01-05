package com.structurizr.export;

import com.structurizr.Workspace;
import com.structurizr.model.*;
import com.structurizr.util.StringUtils;
import com.structurizr.view.*;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractDiagramExporter extends AbstractExporter implements DiagramExporter {

    private Object frame = null;

    /**
     * Exports all views in the workspace.
     *
     * @param workspace     the workspace containing the views to be written
     * @return  a collection of diagram definitions, one per view
     */
    public final Collection<Diagram> export(Workspace workspace) {
        if (workspace == null) {
            throw new IllegalArgumentException("A workspace must be provided.");
        }

        Collection<Diagram> diagrams = new ArrayList<>();

        for (CustomView view : workspace.getViews().getCustomViews()) {
            Diagram diagram = export(view);
            if (diagram != null) {
                diagrams.add(diagram);
            }
        }

        for (SystemLandscapeView view : workspace.getViews().getSystemLandscapeViews()) {
            Diagram diagram = export(view);
            if (diagram != null) {
                diagrams.add(diagram);
            }
        }

        for (SystemContextView view : workspace.getViews().getSystemContextViews()) {
            Diagram diagram = export(view);
            if (diagram != null) {
                diagrams.add(diagram);
            }
        }

        for (ContainerView view : workspace.getViews().getContainerViews()) {
            Diagram diagram = export(view);
            if (diagram != null) {
                diagrams.add(diagram);
            }
        }

        for (ComponentView view : workspace.getViews().getComponentViews()) {
            Diagram diagram = export(view);
            if (diagram != null) {
                diagrams.add(diagram);
            }
        }

        for (DynamicView view : workspace.getViews().getDynamicViews()) {
            Diagram diagram = export(view);
            if (diagram != null) {
                diagrams.add(diagram);
            }
        }

        for (DeploymentView view : workspace.getViews().getDeploymentViews()) {
            Diagram diagram = export(view);
            if (diagram != null) {
                diagrams.add(diagram);
            }
        }

        return diagrams;
    }

    public Diagram export(CustomView view) {
        Diagram diagram = export(view, null);

        if (isAnimationSupported(view) && !view.getAnimations().isEmpty()) {
            for (Animation animation : view.getAnimations()) {
                Diagram frame = export(view, animation.getOrder());
                diagram.addFrame(frame);
            }
        }

        diagram.setLegend(createLegend(view));
        return diagram;
    }

    private Diagram export(CustomView view, Integer animationStep) {
        this.frame = animationStep;

        IndentingWriter writer = new IndentingWriter();
        writeHeader(view, writer);

        List<GroupableElement> elements = new ArrayList<>();
        for (ElementView elementView : view.getElements()) {
            elements.add((CustomElement)elementView.getElement());
        }

        writeElements(view, elements, writer);

        writer.writeLine();
        writeRelationships(view, writer);
        writeFooter(view, writer);

        return createDiagram(view, writer.toString());
    }

    public Diagram export(SystemLandscapeView view) {
        Diagram diagram = export(view, null);

        if (isAnimationSupported(view) && !view.getAnimations().isEmpty()) {
            for (Animation animation : view.getAnimations()) {
                Diagram frame = export(view, animation.getOrder());
                diagram.addFrame(frame);
            }
        }

        diagram.setLegend(createLegend(view));
        return diagram;
    }

    private Diagram export(SystemLandscapeView view, Integer animationStep) {
        this.frame = animationStep;
        return export(view, view.isEnterpriseBoundaryVisible());
    }

    public Diagram export(SystemContextView view) {
        Diagram diagram = export(view, null);

        if (isAnimationSupported(view) && !view.getAnimations().isEmpty()) {
            for (Animation animation : view.getAnimations()) {
                Diagram frame = export(view, animation.getOrder());
                diagram.addFrame(frame);
            }
        }

        diagram.setLegend(createLegend(view));
        return diagram;
    }

    private Diagram export(SystemContextView view, Integer animationStep) {
        this.frame = animationStep;
        return export(view, view.isEnterpriseBoundaryVisible());
    }

    private Diagram export(View view, boolean enterpriseBoundaryIsVisible) {
        IndentingWriter writer = new IndentingWriter();
        writeHeader(view, writer);

        boolean showEnterpriseBoundary =
                enterpriseBoundaryIsVisible &&
                (view.getElements().stream().map(ElementView::getElement).anyMatch(e -> e instanceof Person && ((Person)e).getLocation() == Location.Internal) ||
                 view.getElements().stream().map(ElementView::getElement).anyMatch(e -> e instanceof SoftwareSystem && ((SoftwareSystem)e).getLocation() == Location.Internal));

        if (showEnterpriseBoundary) {
            String enterpriseName = "Enterprise";
            if (view.getModel().getEnterprise() != null) {
                enterpriseName = view.getModel().getEnterprise().getName();
            }

            startEnterpriseBoundary(view, enterpriseName, writer);

            List<GroupableElement> elementsInsideEnterpriseBoundary = new ArrayList<>();
            for (ElementView elementView : view.getElements()) {
                if (elementView.getElement() instanceof Person && ((Person)elementView.getElement()).getLocation() == Location.Internal) {
                    elementsInsideEnterpriseBoundary.add((StaticStructureElement)elementView.getElement());
                }
                if (elementView.getElement() instanceof SoftwareSystem && ((SoftwareSystem)elementView.getElement()).getLocation() == Location.Internal) {
                    elementsInsideEnterpriseBoundary.add((StaticStructureElement)elementView.getElement());
                }
            }
            writeElements(view, elementsInsideEnterpriseBoundary, writer);

            endEnterpriseBoundary(view, writer);

            List<GroupableElement> elementsOutsideEnterpriseBoundary = new ArrayList<>();
            for (ElementView elementView : view.getElements()) {
                if (elementView.getElement() instanceof Person && ((Person)elementView.getElement()).getLocation() != Location.Internal) {
                    elementsOutsideEnterpriseBoundary.add((StaticStructureElement)elementView.getElement());
                }
                if (elementView.getElement() instanceof SoftwareSystem && ((SoftwareSystem)elementView.getElement()).getLocation() != Location.Internal) {
                    elementsOutsideEnterpriseBoundary.add((StaticStructureElement)elementView.getElement());
                }
                if (elementView.getElement() instanceof CustomElement) {
                    elementsOutsideEnterpriseBoundary.add((CustomElement)elementView.getElement());
                }
            }
            writeElements(view, elementsOutsideEnterpriseBoundary, writer);
        } else {
            List<GroupableElement> elements = new ArrayList<>();
            for (ElementView elementView : view.getElements()) {
                elements.add((GroupableElement)elementView.getElement());
            }
            writeElements(view, elements, writer);
        }

        writer.writeLine();
        writeRelationships(view, writer);
        writeFooter(view, writer);

        return createDiagram(view, writer.toString());
    }

    public Diagram export(ContainerView view) {
        Diagram diagram = export(view, null);

        if (isAnimationSupported(view) && !view.getAnimations().isEmpty()) {
            for (Animation animation : view.getAnimations()) {
                Diagram frame = export(view, animation.getOrder());
                diagram.addFrame(frame);
            }
        }

        diagram.setLegend(createLegend(view));
        return diagram;
    }

    public Diagram export(ContainerView view, Integer animationStep) {
        this.frame = animationStep;
        IndentingWriter writer = new IndentingWriter();
        writeHeader(view, writer);

        boolean elementsWritten = false;
        for (ElementView elementView : view.getElements()) {
            if (!(elementView.getElement() instanceof Container)) {
                writeElement(view, elementView.getElement(), writer);
                elementsWritten = true;
            }
        }

        if (elementsWritten) {
            writer.writeLine();
        }

        List<SoftwareSystem> softwareSystems = getBoundarySoftwareSystems(view);
        for (SoftwareSystem softwareSystem : softwareSystems) {
            boolean showSoftwareSystemBoundary = softwareSystem.equals(view.getSoftwareSystem()) || view.getExternalSoftwareSystemBoundariesVisible();
            if (showSoftwareSystemBoundary) {
                startSoftwareSystemBoundary(view, softwareSystem, writer);
            }

            List<GroupableElement> scopedElements = new ArrayList<>();
            for (ElementView elementView : view.getElements()) {
                if (elementView.getElement().getParent() == softwareSystem) {
                    scopedElements.add((StaticStructureElement) elementView.getElement());
                }
            }

            writeElements(view, scopedElements, writer);

            if (showSoftwareSystemBoundary) {
                endSoftwareSystemBoundary(view, writer);
            } else {
                writer.writeLine();
            }
        }

        writeRelationships(view, writer);

        writeFooter(view, writer);

        return createDiagram(view, writer.toString());
    }

    protected List<SoftwareSystem> getBoundarySoftwareSystems(View view) {
        List<SoftwareSystem> softwareSystems = new ArrayList<>(view.getElements().stream().map(ElementView::getElement).filter(e -> e instanceof Container).map(c -> ((Container)c).getSoftwareSystem()).collect(Collectors.toSet()));
        softwareSystems.sort(Comparator.comparing(Element::getId));

        return softwareSystems;
    }

    public Diagram export(ComponentView view) {
        Diagram diagram = export(view, null);

        if (isAnimationSupported(view) && !view.getAnimations().isEmpty()) {
            for (Animation animation : view.getAnimations()) {
                Diagram frame = export(view, animation.getOrder());
                diagram.addFrame(frame);
            }
        }

        diagram.setLegend(createLegend(view));
        return diagram;
    }

    public Diagram export(ComponentView view, Integer animationStep) {
        this.frame = animationStep;
        IndentingWriter writer = new IndentingWriter();
        writeHeader(view, writer);

        boolean elementsWritten = false;
        for (ElementView elementView : view.getElements()) {
            if (!(elementView.getElement() instanceof Component)) {
                writeElement(view, elementView.getElement(), writer);
                elementsWritten = true;
            }
        }

        if (elementsWritten) {
            writer.writeLine();
        }

        List<Container> containers = getBoundaryContainers(view);
        for (Container container : containers) {
            boolean showContainerBoundary = container.equals(view.getContainer()) || view.getExternalContainerBoundariesVisible();
            if (showContainerBoundary) {
                startContainerBoundary(view, container, writer);
            }

            List<GroupableElement> scopedElements = new ArrayList<>();
            for (ElementView elementView : view.getElements()) {
                if (elementView.getElement().getParent() == container) {
                    scopedElements.add((StaticStructureElement) elementView.getElement());
                }
            }
            writeElements(view, scopedElements, writer);

            if (showContainerBoundary) {
                endContainerBoundary(view, writer);
            } else {
                writer.writeLine();
            }
        }

        writeRelationships(view, writer);

        writeFooter(view, writer);

        return createDiagram(view, writer.toString());
    }

    protected List<Container> getBoundaryContainers(View view) {
        List<Container> containers = new ArrayList<>(view.getElements().stream().map(ElementView::getElement).filter(e -> e instanceof Component).map(c -> ((Component)c).getContainer()).collect(Collectors.toSet()));
        containers.sort(Comparator.comparing(Element::getId));

        return containers;
    }

    public Diagram export(DynamicView view) {
        Diagram diagram = export(view, null);

        if (isAnimationSupported(view)) {
            LinkedHashSet<String> orders = new LinkedHashSet<>();
            for (RelationshipView relationshipView : view.getRelationships()) {
                orders.add(relationshipView.getOrder());
            }

            for (String order : orders) {
                Diagram frame = export(view, order);
                diagram.addFrame(frame);
            }
        }

        diagram.setLegend(createLegend(view));
        return diagram;
    }

    public Diagram export(DynamicView view, String order) {
        this.frame = order;
        IndentingWriter writer = new IndentingWriter();
        writeHeader(view, writer);

        boolean elementsWritten = false;

        Element element = view.getElement();

        if (element == null) {
            for (ElementView elementView : view.getElements()) {
                writeElement(view, elementView.getElement(), writer);
                elementsWritten = true;
            }
        } else {
            if (element instanceof SoftwareSystem) {
                List<SoftwareSystem> softwareSystems = getBoundarySoftwareSystems(view);

                for (SoftwareSystem softwareSystem : softwareSystems) {
                    boolean showSoftwareSystemBoundary = softwareSystem.equals(view.getElement()) || view.getExternalBoundariesVisible();

                    if (showSoftwareSystemBoundary) {
                        startSoftwareSystemBoundary(view, softwareSystem, writer);
                    }

                    for (ElementView elementView : view.getElements()) {
                        if (elementView.getElement().getParent() == softwareSystem) {
                            writeElement(view, elementView.getElement(), writer);
                        }
                    }

                    if (showSoftwareSystemBoundary) {
                        endSoftwareSystemBoundary(view, writer);
                    } else {
                        writer.writeLine();
                    }
                }

                for (ElementView elementView : view.getElements()) {
                    if (elementView.getElement().getParent() == null) {
                        writeElement(view, elementView.getElement(), writer);
                        elementsWritten = true;
                    }
                }
            } else if (element instanceof Container) {
                List<Container> containers = getBoundaryContainers(view);

                for (Container container : containers) {
                    boolean showContainerBoundary = container.equals(view.getElement()) || view.getExternalBoundariesVisible();

                    if (showContainerBoundary) {
                        startContainerBoundary(view, container, writer);
                    }

                    for (ElementView elementView : view.getElements()) {
                        if (elementView.getElement().getParent() == container) {
                            writeElement(view, elementView.getElement(), writer);
                        }
                    }

                    if (showContainerBoundary) {
                        endContainerBoundary(view, writer);
                    } else {
                        writer.writeLine();
                    }
                }

                for (ElementView elementView : view.getElements()) {
                    if (!(elementView.getElement().getParent() instanceof Container)) {
                        writeElement(view, elementView.getElement(), writer);
                        elementsWritten = true;
                    }
                }
            }
        }

        if (elementsWritten) {
            writer.writeLine();
        }

        writeRelationships(view, writer);
        writeFooter(view, writer);

        return createDiagram(view, writer.toString());
    }

    public Diagram export(DeploymentView view) {
        Diagram diagram = export(view, null);

        if (isAnimationSupported(view) && !view.getAnimations().isEmpty()) {
            for (Animation animation : view.getAnimations()) {
                Diagram frame = export(view, animation.getOrder());
                diagram.addFrame(frame);
            }
        }

        diagram.setLegend(createLegend(view));
        return diagram;
    }

    public Diagram export(DeploymentView view, Integer animationStep) {
        this.frame = animationStep;
        IndentingWriter writer = new IndentingWriter();
        writeHeader(view, writer);

        for (ElementView elementView : view.getElements()) {
            if (elementView.getElement() instanceof DeploymentNode && elementView.getElement().getParent() == null) {
                write(view, (DeploymentNode)elementView.getElement(), writer);
            }
        }

        writeRelationships(view, writer);
        writeFooter(view, writer);

        return createDiagram(view, writer.toString());
    }

    private void write(DeploymentView view, DeploymentNode deploymentNode, IndentingWriter writer) {
        startDeploymentNodeBoundary(view, deploymentNode, writer);

        List<DeploymentNode> children = new ArrayList<>(deploymentNode.getChildren());
        children.sort(Comparator.comparing(DeploymentNode::getName));
        for (DeploymentNode child : children) {
            if (view.isElementInView(child)) {
                write(view, child, writer);

            }
        }

        List<InfrastructureNode> infrastructureNodes = new ArrayList<>(deploymentNode.getInfrastructureNodes());
        infrastructureNodes.sort(Comparator.comparing(InfrastructureNode::getName));
        for (InfrastructureNode infrastructureNode : infrastructureNodes) {
            if (view.isElementInView(infrastructureNode)) {
                writeElement(view, infrastructureNode, writer);
            }
        }

        List<SoftwareSystemInstance> softwareSystemInstances = new ArrayList<>(deploymentNode.getSoftwareSystemInstances());
        softwareSystemInstances.sort(Comparator.comparing(SoftwareSystemInstance::getName));
        for (SoftwareSystemInstance softwareSystemInstance : softwareSystemInstances) {
            if (view.isElementInView(softwareSystemInstance)) {
                writeElement(view, softwareSystemInstance, writer);
            }
        }

        List<ContainerInstance> containerInstances = new ArrayList<>(deploymentNode.getContainerInstances());
        containerInstances.sort(Comparator.comparing(ContainerInstance::getName));
        for (ContainerInstance containerInstance : containerInstances) {
            if (view.isElementInView(containerInstance)) {
                writeElement(view, containerInstance, writer);
            }
        }

        endDeploymentNodeBoundary(view, writer);
    }

    protected void writeElements(View view, List<GroupableElement> elements, IndentingWriter writer) {
        elements.sort(Comparator.comparing(Element::getId));

        Set<String> groupsAsSet = new HashSet<>();
        for (GroupableElement element : elements) {
            String group = element.getGroup();

            if (!StringUtils.isNullOrEmpty(group)) {
                groupsAsSet.add(group);
            }
        }

        List<String> groupsAsList = new ArrayList<>(groupsAsSet);
        Collections.sort(groupsAsList);

        // first render grouped elements
        for (String group : groupsAsList) {
            startGroupBoundary(view, group, writer);

            for (GroupableElement element : elements) {
                if (group.equals(element.getGroup())) {
                    writeElement(view, element, writer);
                }
            }

            endGroupBoundary(view, writer);
        }

        // then render ungrouped elements
        for (GroupableElement element : elements) {
            if (StringUtils.isNullOrEmpty(element.getGroup())) {
                writeElement(view, element, writer);
            }
        }
    }

    protected void writeRelationships(View view, IndentingWriter writer) {
        Collection<RelationshipView> relationshipList;

        if (view instanceof DynamicView) {
            relationshipList = view.getRelationships();
        } else {
            relationshipList = view.getRelationships().stream().sorted(Comparator.comparing(rv -> rv.getRelationship().getId())).collect(Collectors.toList());
        }

        for (RelationshipView relationshipView : relationshipList) {
            System.out.println(relationshipView);
            writeRelationship(view, relationshipView, writer);
        }
    }

    protected abstract void writeHeader(View view, IndentingWriter writer);
    protected abstract void writeFooter(View view, IndentingWriter writer);

    protected abstract void startEnterpriseBoundary(View view, String enterpriseName, IndentingWriter writer);
    protected abstract void endEnterpriseBoundary(View view, IndentingWriter writer);

    protected abstract void startGroupBoundary(View view, String group, IndentingWriter writer);
    protected abstract void endGroupBoundary(View view, IndentingWriter writer);

    protected abstract void startSoftwareSystemBoundary(View view, SoftwareSystem softwareSystem, IndentingWriter writer);
    protected abstract void endSoftwareSystemBoundary(View view, IndentingWriter writer);

    protected abstract void startContainerBoundary(View view, Container container, IndentingWriter writer);
    protected abstract void endContainerBoundary(View view, IndentingWriter writer);

    protected abstract void startDeploymentNodeBoundary(DeploymentView view, DeploymentNode deploymentNode, IndentingWriter writer);
    protected abstract void endDeploymentNodeBoundary(View view, IndentingWriter writer);

    protected abstract void writeElement(View view, Element element, IndentingWriter writer);
    protected abstract void writeRelationship(View view, RelationshipView relationshipView, IndentingWriter writer);

    protected boolean isAnimationSupported(View view) {
        return false;
    }

    protected boolean isVisible(View view, Element element) {
        if (frame != null) {
            Set<String> elementIds = new HashSet<>();

            if (view instanceof StaticView) {
                int step = (int)frame;
                if (step > 0) {
                    StaticView staticView = (StaticView) view;
                    staticView.getAnimations().stream().filter(a -> a.getOrder() <= step).forEach(a -> {
                        elementIds.addAll(a.getElements());
                    });

                    return elementIds.contains(element.getId());
                }
            } else if (view instanceof DeploymentView) {
                int step = (int)frame;
                if (step > 0) {
                    DeploymentView deploymentView = (DeploymentView) view;
                    deploymentView.getAnimations().stream().filter(a -> a.getOrder() <= step).forEach(a -> {
                        elementIds.addAll(a.getElements());
                    });

                    return elementIds.contains(element.getId());
                }
            } else if (view instanceof DynamicView) {
                String order = (String)frame;
                view.getRelationships().stream().filter(rv -> order.equals(rv.getOrder())).forEach(rv -> {
                    elementIds.add(rv.getRelationship().getSourceId());
                    elementIds.add(rv.getRelationship().getDestinationId());
                });

                return elementIds.contains(element.getId());
            }
        }

        return true;
    }

    protected boolean isVisible(View view, RelationshipView relationshipView) {
        if (view instanceof DynamicView && frame != null) {
            return frame.equals(relationshipView.getOrder());
        }

        return true;
    }

    protected abstract Diagram createDiagram(View view, String definition);

    protected Legend createLegend(View view) {
        return null;
    }

    protected String getViewOrViewSetProperty(View view, String name, String defaultValue) {
        ViewSet views = view.getViewSet();

        return
            view.getProperties().getOrDefault(name,
                    views.getConfiguration().getProperties().getOrDefault(name, defaultValue)
            );
    }

}
