package fna.charactermarkup;

import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
/*
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;*/

import fna.parsing.ParsingException;

public class CompareXML {

	private static int structexactmatch = 0;
	private static int structpartmatch = 0;
	private static int structnomatch = 0;
	private static int charexactmatch = 0;
	private static int charpartmatch = 0;
	private static int charnomatch = 0;
	private static int relexactmatch = 0;
	private static int relpartmatch = 0;
	private static int relnomatch = 0;
	
	public CompareXML() {
		try{
		// TODO Auto-generated constructor stub
			SAXBuilder builder = new SAXBuilder();
			Document anskey = builder.build("F:\\UA\\RA\\Code\\parsing-gui-charactermarkup\\anskey.xml");
			Element ansroot = anskey.getRootElement();
			Document testcase = builder.build("F:\\UA\\RA\\Code\\parsing-gui-charactermarkup\\testcase.xml");
			Element testroot = testcase.getRootElement();
			validatestruct(ansroot, testroot);
			validatecharacter(ansroot, testroot);
			validaterelation(ansroot, testroot);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void validatestruct(Element ansroot, Element testroot) {
		String exact = "";
		List ansli = ansroot.getChildren("structure");
		List testli = testroot.getChildren("structure");
		if (ansli.size() == testli.size()){
			for(int i = 0; i < testli.size(); i++){
				Element ans = (Element)ansli.get(i);
				Element test = (Element)testli.get(i);
				if (test.getAttributeValue("name").compareTo(ans.getAttributeValue("name"))==0){
					structexactmatch++;
					exact += test.getAttributeValue("id");
				}
				else if (ans.getAttributeValue("name").contains(test.getAttributeValue("name"))|test.getAttributeValue("name").contains(ans.getAttributeValue("name"))){
					structpartmatch++;
					exact += test.getAttributeValue("id");
				}
			}
			if ((structexactmatch+structpartmatch) == testli.size()-1){
				structnomatch = 1;
			}	
			else if ((structexactmatch+structpartmatch) < testli.size()-1){
				for(int i = 0; i < testli.size(); i++){
					int flag = 0;
					Element test = (Element)testli.get(i);
					if(!exact.contains(test.getAttributeValue("id"))){
						for(int j = 0; j < ansli.size(); j++){
							Element ans = (Element)ansli.get(j);
							if(i!=j && !exact.contains(ans.getAttributeValue("id"))){
								if (test.getAttributeValue("name").compareTo(ans.getAttributeValue("name"))==0 | ans.getAttributeValue("name").contains(test.getAttributeValue("name")) | test.getAttributeValue("name").contains(ans.getAttributeValue("name"))){
									structpartmatch++;
									flag = 1;
									break;
								}
							}
						}
						if(flag == 0)
							structnomatch++;
					}
				}
			}
		}	
		else{
			int len = 0;
			if(ansli.size() < testli.size())
				len =ansli.size();
			else
				len = testli.size();
			for(int i = 0; i < len; i++){
				Element ans = (Element)ansli.get(i);
				Element test = (Element)testli.get(i);
				if (test.getAttributeValue("name").compareTo(ans.getAttributeValue("name"))==0){
					structexactmatch++;
					exact += test.getAttributeValue("id");
				}
				else if (ans.getAttributeValue("name").contains(test.getAttributeValue("name"))|test.getAttributeValue("name").contains(ans.getAttributeValue("name"))){
					structpartmatch++;
					exact += test.getAttributeValue("id");
				}
			}
			for(int i = 0; i < testli.size(); i++){
				int flag = 0;
				Element test = (Element)testli.get(i);
				if(!exact.contains(test.getAttributeValue("id"))){
					for(int j = 0; j < ansli.size(); j++){
						Element ans = (Element)ansli.get(j);
						if(i!=j && !exact.contains(ans.getAttributeValue("id"))){
							if (test.getAttributeValue("name").compareTo(ans.getAttributeValue("name"))==0 | ans.getAttributeValue("name").contains(test.getAttributeValue("name")) | test.getAttributeValue("name").contains(ans.getAttributeValue("name"))){
								structpartmatch++;
								flag = 1;
								break;
							}
						}
					}
					if(flag == 0)
						structnomatch++;
				}
			}
		}
		System.out.println("structexactmatch:"+structexactmatch);
		System.out.println("structpartmatch:"+structpartmatch);
		System.out.println("structnomatch:"+structnomatch);
	}
		
	private void validatecharacter(Element ansroot, Element testroot) {
		String exact = "";
		List structansli = ansroot.getChildren("structure");
		List structtestli = testroot.getChildren("structure");
		List ansli = null, testli = null;
		int flags = 0;
		for( Iterator l = structansli.iterator(); l.hasNext();){
			Element structans = (Element)l.next();
			if(!structans.getChildren().isEmpty()){
				if(flags == 0){
					ansli = structans.getChildren();
					flags = 1;
				}
				else
					ansli.addAll(structans.getChildren());
			}
		}
		flags = 0;
		for( Iterator l = structtestli.iterator(); l.hasNext();){
			Element structtest = (Element)l.next();
			if(!structtest.getChildren().isEmpty()){
				if(flags == 0){
					testli = structtest.getChildren();
					flags = 1;
				}
				else
					testli.addAll(structtest.getChildren());
			}
		}
		//System.out.println(ansli.size());
		//System.out.println(testli.size());
		for(int i = 0; i < testli.size(); i++){
			int flag = 0;
			Element test = (Element)testli.get(i);
			for(int j = 0; j < ansli.size(); j++){
				flag = 0;
				Element ans = (Element)ansli.get(j);
				//System.out.println(test.getParentElement().getAttributeValue("id"));
				//System.out.println(ans.getParentElement().getAttributeValue("id"));
				if (test.getParentElement().getAttributeValue("id").compareTo(ans.getParentElement().getAttributeValue("id"))==0){
					List testattr = test.getAttributes();
					List ansattr = ans.getAttributes();
					if (testattr.size() == ansattr.size()){
						for(Iterator k = testattr.iterator(); k.hasNext();){
							Attribute a = (Attribute)k.next();
							//System.out.println(ansattr.toString());
							//System.out.println(a.toString());
							if(!ansattr.toString().contains(a.toString())){
								flag = 1;
								break;
							}
						}
						if(flag == 0){
							charexactmatch++;
							exact += test.getAttributes().toString();
							ansli.remove(j);
							break;
						}
					}
				}
			}
		}
		
		for(int i = 0; i < testli.size(); i++){
			int flag = 0;
			Element test = (Element)testli.get(i);
			if(!exact.contains(test.getAttributes().toString())){
				for(int j = 0; j < ansli.size(); j++){
					Element ans = (Element)ansli.get(j);
					if (test.getParentElement().getAttributeValue("id").compareTo(ans.getParentElement().getAttributeValue("id"))==0){
						int count = 0, passct = 0;
						List testattr = test.getAttributes();
						List ansattr = ans.getAttributes();
						for(int k = 0; k < testattr.size(); k++){
							Attribute atest = (Attribute)testattr.get(k);
							for(int l = 0; l < ansattr.size(); l++){
								Attribute aans = (Attribute)ansattr.get(l);
								if(atest.getName().toString().compareTo(aans.getName().toString())==0){
									passct++;
									if(!aans.getValue().contains(atest.getValue())|!atest.getValue().contains(aans.getValue())){
										count++;
										break;
									}
									else
										break;
								}
							}
						}
						if(count == 1 && passct!=1){
							charpartmatch++;
							flag = 1;
							break;
						}
						else if(count == 0 && (passct==testattr.size()|passct==testattr.size()-1)){
							charpartmatch++;
							flag = 1;
							break;
						}
					}
				}
				if (flag == 0){
					charnomatch++;
				}
			}
		}
		System.out.println("charexactmatch:"+charexactmatch);
		System.out.println("charpartmatch:"+charpartmatch);
		System.out.println("charnomatch:"+charnomatch);
		
	}
	
	private void validaterelation(Element ansroot, Element testroot) {
		String exact = "";
		List ansli = ansroot.getChildren("relation");
		List testli = testroot.getChildren("relation");
		for(int i = 0; i < testli.size(); i++){
			int flag = 0;
			Element test = (Element)testli.get(i);
			for(int j = 0; j < ansli.size(); j++){
				flag = 0;
				Element ans = (Element)ansli.get(j);
				List testattr = test.getAttributes();
				List ansattr = ans.getAttributes();
				for(int k = 1; k < testattr.size(); k++){
					Attribute a = (Attribute)testattr.get(k);
					//System.out.println(ansattr.toString());
					//System.out.println(a.toString());
					if(!ansattr.toString().contains(a.toString())){
						flag = 1;
						break;
					}
				}
				if(flag == 0){
					relexactmatch++;
					exact += test.getAttributeValue("id");
					ansli.remove(j);
					break;
				}
			}
		}
		
		for(int i = 0; i < testli.size(); i++){
			int flag = 0;
			Element test = (Element)testli.get(i);
			if(!exact.contains(test.getAttributeValue("id"))){
				for(int j = 0; j < ansli.size(); j++){
					flag = 0;
					int ct = 0;
					Element ans = (Element)ansli.get(j);
					List testattr = test.getAttributes();
					List ansattr = ans.getAttributes();
					if(ans.getAttributeValue("negation").contains(test.getAttributeValue("negation"))){
						if(ans.getAttributeValue("name").contains(test.getAttributeValue("name"))|test.getAttributeValue("name").contains(ans.getAttributeValue("name"))){
							for(int k = 1; k < 3; k++){
								Attribute aans = (Attribute)ansattr.get(k);
								Attribute atest = (Attribute)testattr.get(k);
								if(!aans.getValue().contains(atest.getValue())){
									ct++;
								}
							}
							if(ct == 0|ct == 1){
								relpartmatch++;
								flag = 1;
								break;
							}
						}
					}
				}
				if(flag == 0){
					relnomatch++;
				}
			}
		}
		System.out.println("relexactmatch:"+relexactmatch);
		System.out.println("relpartmatch:"+relpartmatch);
		System.out.println("relnomatch:"+relnomatch);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CompareXML cXML = new CompareXML();
	}

}
