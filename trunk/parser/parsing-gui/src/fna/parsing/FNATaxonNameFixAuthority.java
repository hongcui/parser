package fna.parsing;

import org.jdom.xpath.XPath;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

public class FNATaxonNameFixAuthority {
	private String inputFolder = "E:\\work_data\\ToReview\\V8-good\\target\\Fengqiong_manually_reviewed";
	private XPath xpath1 = XPath.newInstance("//TaxonIdentification");

	public FNATaxonNameFixAuthority() throws Exception {
		File xmlfolder = new File(inputFolder);
		File[] xmls = xmlfolder.listFiles();
		int count = 0;
		for (File xml : xmls) {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(xml);
			Element root = doc.getRootElement();
			List<Element> taxons = xpath1.selectNodes(root);

			for (Element taxon : taxons) {
				if (taxon != null) {
					Element authority = null;
					Element place_of_publication = null;
					Element publication_title = null;
					String str_authority = "";
					String addTo_publication = "";
					String str_publication = "";
					boolean neetToFix = false;
					List<Element> children = taxon.getChildren();

					for (Element eachChild : children) {
						if (eachChild != null) {
							String tagName = eachChild.getName();

							// authority
							if (tagName.contains("_authority")) {
								authority = eachChild;
								str_authority = eachChild.getValue();

								// contains " in "
								if (str_authority.contains(" in ")) {
									neetToFix = true;									
								} else {
									// no need to fix
									break;
								}
							} else if (tagName.equals("place_of_publication")) {
								// this is the publication parent
								place_of_publication = eachChild;
								publication_title = place_of_publication.getChild("publication_title");
								if (publication_title != null) {
									str_publication = publication_title.getValue();
								}
							}
						}
					}

					if (neetToFix) {
						// update authority
						System.out.println("------" + count++ +"------------------" + xml.getName());
						System.out.println("old authority: " + str_authority);
						System.out.println("old publication title: " + str_publication);
						String new_authority = str_authority.substring(0,
								str_authority.indexOf(" in "));
						authority.setText(new_authority);
						System.out.println("new authority: " + new_authority);
						addTo_publication = str_authority.substring(
								str_authority.indexOf(" in ") + 1,
								str_authority.length());
						
						String new_publication_title = addTo_publication;
						if (!str_publication.equals("")) {
							new_publication_title = addTo_publication + ", "
									+ str_publication;							
						}
						
						if (place_of_publication == null) {
							System.out.println("place_of_publication is null");
							place_of_publication = new Element(
									"place_of_publication");
							taxon.addContent(place_of_publication);
						}

						if (publication_title == null) {
							System.out.println("publication_title is null");
							publication_title = new Element(
									"publication_title");
							place_of_publication
									.addContent(publication_title);
						}

						publication_title.setText(new_publication_title);
						System.out.println("new publication_title: " + new_publication_title);
						output(root, xml.getName());
					}
				}
			}

		}
	}

	private void output(Element treatment, String filename) throws Exception {
		XMLOutputter outputter = new XMLOutputter();		
		String file = inputFolder + "\\" + filename;
		Element newEle = (Element) treatment.clone();
		Document doc = new Document(newEle);
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(file));
		outputter.output(doc, out);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			FNATaxonNameFixAuthority fnr = new FNATaxonNameFixAuthority();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
