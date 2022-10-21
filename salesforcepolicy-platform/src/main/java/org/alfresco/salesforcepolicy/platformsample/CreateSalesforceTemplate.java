package org.alfresco.salesforcepolicy.platformsample;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.alfresco.api.AlfrescoPublicApi;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import java.util.List;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.cmr.repository.*;

public class CreateSalesforceTemplate implements NodeServicePolicies.OnCreateNodePolicy {

    private static final String DOCUMENT_LIBRARY = "documentLibrary";


    //Dependencies
    private NodeService nodeService;

    private PolicyComponent policyComponent;

    private ContentService contentService;

    private SearchService searchService;

    private FileFolderService fileFolderService;

    private SiteService siteService;

    private CopyService copyService;
    //Behaviours
    private Behaviour onCreateNode;

    //initialize Logger
  //  private Logger logger = Logger.getLogger(CreateSalesforceTemplate.class);
    private static Log logger = LogFactory.getLog(CreateSalesforceTemplate.class);

    //Alfresco has to know that the behavior needs to be bound to a policy.
    //init() will handle the binding. It will get called when Spring loads the bean.
    public void init()
    {
        System.out.println("Here");
        if (logger.isDebugEnabled()) logger.debug("Initializing Custom behaviors");
       //Create OnCreate behavior
        this.onCreateNode = new JavaBehaviour(this,"onCreateNode",Behaviour.NotificationFrequency.TRANSACTION_COMMIT);

        //bind Behavior to node policy
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onCreateNode"),
                ContentModel.TYPE_CONTENT, this.onCreateNode);
    }

    @Override
    public void onCreateNode(ChildAssociationRef childAssocRef)
    {
        //Logging Inside OnCreate
        if (logger.isDebugEnabled()) logger.debug("Inside onCreateNode for Create Salesforce Template");

        //Check For Salesforce Object Node Type
        NodeRef nodeRef = childAssocRef.getChildRef();

        //If Salesforce Node Type, then Create Folder Structure
        if(nodeService.getType(nodeRef).toString().compareTo("{http://www.alfresco.org/model/content/1.0}folder")  == 0 )
        {
            if (logger.isDebugEnabled()) logger.debug("Nodestore: " + nodeRef.getStoreRef().toString() );

            if (logger.isDebugEnabled()) logger.debug("Path: " + nodeService.getPaths(nodeRef,true).toString());

            createFolderStructure(nodeRef);

        } else if (nodeService.getType(nodeRef).toString().compareTo("{http://www.alfresco.org/model/content/1.0}content") == 0)
        {
            if (logger.isDebugEnabled()) logger.debug("This is Content & not a Folder");
            createFolderStructure(nodeRef);
        }
        else
        {
            if (logger.isDebugEnabled()) logger.debug("This is neither Content nor a Folder");
        }
    }

    public void createFolderStructure(NodeRef nodeRef)
    {
        if (logger.isDebugEnabled()) logger.debug("Inside CreateFolderStructure for Create Salesforce Template");



        String sitePreset = "Test";

        //see if there is a folder in the Space Templates folder of the same name
        String query = "+PATH:\"/app:company_home/app:dictionary/cm:Site_x0020_Folder_x0020_Templates/*\"+@cm\\:name:\"" + sitePreset + "\"";
        if (logger.isDebugEnabled()) logger.debug(query);


        if (logger.isDebugEnabled()) logger.debug("Storeref: "+ StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.toString());
        ResultSet rs = searchService.query(nodeRef.getStoreRef(), SearchService.LANGUAGE_LUCENE, query);


        if (rs.length() == 0)
        {
            query = "+PATH:\"/app:company_home/app:dictionary/app:space_templates/*\"+@cm\\:name:\"" + sitePreset + "\"";
            rs = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_LUCENE, query);
        }

        if (rs.length() <= 0)
        {
            logger.debug("Found no space templates for: " + sitePreset);
            return;
        }/* else if (!templateName.equals(sitePreset))
        {
            logger.debug("Space template name is not an exact match: " + templateName);
            spaceTemplate = null;
            continue;
        } else
        {
            break;
        }*/


        NodeRef spaceTemplate = null;
        for (int i = 0; i < rs.length(); i++) {
            spaceTemplate = rs.getNodeRef(i);
            if (!nodeService.exists(spaceTemplate)) {
                spaceTemplate = null;
                continue;
            } else {
                //confirm that the space template's name is an exact match -- Issue #3
                String templateName = (String) nodeService.getProperty(spaceTemplate, ContentModel.PROP_NAME);
                if (!templateName.equals(sitePreset)) {
                    logger.debug("Space template name is not an exact match: " + templateName);
                    spaceTemplate = null;
                    continue;
                } else {
                    break;
                }
            }
        }

        if (spaceTemplate == null) {
            logger.debug("Space template doesn't exist");
            return;
        } else {
            logger.debug("Found space template: " + nodeService.getProperty(spaceTemplate, ContentModel.PROP_NAME));
        }
        // otherwise, create the documentLibrary folder
        String siteId = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
        logger.debug("Site ID: " + siteId);

        // use the site service to do this so that permissions get set correctly
//        NodeRef documentLibrary = siteService.getContainer(siteId, DOCUMENT_LIBRARY);
        NodeRef documentLibrary = siteService.getContainer("test2", DOCUMENT_LIBRARY);

        logger.debug("document library: "+ siteService.getSiteShortName(documentLibrary));

        // now, for each child in the space template, do a copy to the documentLibrary
        List<ChildAssociationRef> children = nodeService.getChildAssocs(spaceTemplate);
        if (children != null)
        {
            logger.debug("Children Node Available");
        }
        else
        {
            logger.debug("No Child Nodes");
        }
        for (ChildAssociationRef childRef : children) {
            // we only want contains associations
            logger.debug("Checking Associations");
            if (childRef.getQName().equals(ContentModel.ASSOC_CONTAINS)) {
                logger.debug("Association Check: YES");
                continue;
            }
            else {
                logger.debug("Child Ref: " + childRef.getQName().toString());
            }
            NodeRef child = childRef.getChildRef();

            //Get child nodes for test site
            List<ChildAssociationRef> library = nodeService.getChildAssocs(documentLibrary);
            if (library != null)
            {
                for (ChildAssociationRef libchild : library)
                {
                    logger.debug("library site children: " + libchild.getQName());
                }
            }

            logger.debug("Space Template Folders " + childRef.getQName().toString());

            logger.debug("Child to Copy " + child.toString());
            //logger.debug(siteService.hasContainer(documen,documentLibrary.toString());
            try {
                fileFolderService.copy(child, documentLibrary, childRef.getQName().getLocalName());
                logger.debug("Successfully copied a child node from the template");
            } catch (FileExistsException e) {
                logger.debug("The child node already exists in the document library.");
            } catch (FileNotFoundException e) {
                //can't find the space template, just bail
                logger.warn("Share site tried to use a space template, but the source space template could not be found.");
            }
        }
    }
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }

    public void setPolicyComponent(PolicyComponent policyComponent) {
        this.policyComponent = policyComponent;
    }
    public void setSiteService(SiteService siteService) {
        this.siteService = siteService;
    }

    public void setCopyService(CopyService copyService) {
        this.copyService = copyService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }
    public SiteService getSiteService() {
        return siteService;
    }
}
