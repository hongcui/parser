package fna.charactermarkup;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/*
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;*/

import fna.parsing.ParsingException;

public class CompareXML {

	protected static Connection conn = null;
	protected static String database = null;
	protected static String username = "root";
	protected static String password = "";
	private static int structexactmatch = 0;
	private static int structpartmatch = 0;
	private static int structnomatch = 0;
	private static int structperfmatch = 0;
	private static int charexactmatch = 0;
	private static int charpartmatch = 0;
	private static int charnomatch = 0;
	private static int charperfmatch = 0;
	private static int relexactmatch = 0;
	private static int relpartmatch = 0;
	private static int relnomatch = 0;
	private static int relperfmatch = 0;
	private static int totmachinest = 0;
	private static int totmachinech = 0;
	private static int totmachinerel = 0;
	private static int tothumanst = 0;
	private static int tothumanch = 0;
	private static int tothumanrel = 0;
	private static int dscstructexactmatch = 0;
	private static int dscstructpartmatch = 0;
	private static int dscstructnomatch = 0;
	private static int dscstructperfmatch = 0;
	private static int dsccharexactmatch = 0;
	private static int dsccharpartmatch = 0;
	private static int dsccharnomatch = 0;
	private static int dsccharperfmatch = 0;
	private static int dscrelexactmatch = 0;
	private static int dscrelpartmatch = 0;
	private static int dscrelnomatch = 0;
	private static int dscrelperfmatch = 0;
	private static int dsctotmachinest = 0;
	private static int dsctotmachinech = 0;
	private static int dsctotmachinerel = 0;
	private static int dsctothumanst = 0;
	private static int dsctothumanch = 0;
	private static int dsctothumanrel = 0;
	
	public void collect(String database){
		CompareXML.database = database;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("create table if not exists precisionrecall (source varchar(100) NOT NULL, pperfst Float(5,2), pexactst Float(5,2), ppartialst Float(5,2), preasonst Float(5,2), " +
						"pperfch Float(5,2), pexactch Float(5,2), ppartialch Float(5,2), preasonch Float(5,2), pperfrel Float(5,2), pexactrel Float(5,2), ppartialrel Float(5,2), preasonrel Float(5,2), " +
						"rperfst Float(5,2), rexactst Float(5,2), rpartialst Float(5,2), rreasonst Float(5,2), rperfch Float(5,2), rexactch Float(5,2), rpartialch Float(5,2), rreasonch Float(5,2), " +
						"rperfrel Float(5,2), rexactrel Float(5,2), rpartialrel Float(5,2), rreasonrel Float(5,2), sentprecision Float(5,2),sentrecall Float(5,2), PRIMARY KEY(source))");
				stmt.execute("delete from precisionrecall");
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public CompareXML() {
		try{
			
			//Pass the database whose sentence are to  be evaluated
			collect("fnav19_benchmark");
			
			//URL of folder containing the human annotated files 
			File ansdirectory = new File("F:\\UA\\RA\\AnsKey_Benchmark_sentence");
			String ansfilename[] = ansdirectory.list();
			
			//URL of folder containing the machine generated files
			File testdirectory = new File("F:\\UA\\RA\\TestCase_Benchmark_sentence");
			String testfilename[] = testdirectory.list();
			
			for (int i = 0; i < ansfilename.length; i++) {
				for (int j = 0; j < testfilename.length; j++) {
					if(ansfilename[i].compareTo(testfilename[j])==0){
						SAXBuilder builder = new SAXBuilder();
						System.out.println(ansfilename[i]);
						Document anskey = builder.build("F:\\UA\\RA\\AnsKey_Benchmark_sentence\\"+ansfilename[i]);
						Element ansroot = anskey.getRootElement();
						ansroot = ansroot.getChild("statement");
						Document testcase = builder.build("F:\\UA\\RA\\TestCase_Benchmark_sentence\\"+testfilename[j]);
						Element testroot = testcase.getRootElement();
						structexactmatch = 0;
						structpartmatch = 0;
						structnomatch = 0;
						structperfmatch = 0;
						charexactmatch = 0;
						charpartmatch = 0;
						charnomatch = 0;
						charperfmatch = 0;
						relexactmatch = 0;
						relpartmatch = 0;
						relnomatch = 0;
						relperfmatch = 0;
						totmachinest = 0;
						totmachinech = 0;
						totmachinerel = 0;
						tothumanst = 0;
						tothumanch = 0;
						tothumanrel = 0;
						validatestruct(ansroot, testroot);
						validatecharacter(ansroot, testroot);
						validaterelation(ansroot, testroot);
						calcprecisionrecall(testfilename[j].substring(0, testfilename[j].lastIndexOf('.')));
						break;
					}
				}
			}
			
			dsccalcprecisionrecall();
			
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Performs comparison between the <structure> elements present in machine & human annotated sentence.  
	 * @param ansroot
	 * @param testroot
	 */	
	public void validatestruct(Element ansroot, Element testroot) {
		String exact = "";
		String ansexact = "";
		List ansli = ansroot.getChildren("structure");
		List testli = testroot.getChildren("structure");
		totmachinest = testli.size();
		tothumanst = ansli.size();
		for(int i = 0; i < testli.size(); i++){
			Element test = (Element)testli.get(i);
			for(int j = 0; j < ansli.size(); j++){
				Element ans = (Element)ansli.get(j);
				if(!ansexact.contains(ans.getAttributeValue("id"))){
					if (test.getAttributeValue("name").compareTo(ans.getAttributeValue("name"))==0){
						if (test.getAttribute("constraint")!=null && ans.getAttribute("constraint")!=null){
							if (test.getAttributeValue("constraint").compareTo(ans.getAttributeValue("constraint"))==0){
								structexactmatch++;
								structperfmatch++;
								exact += test.getAttributeValue("id");
								ansexact += ans.getAttributeValue("id");
								break;
							}
						}
						else{
							if(test.getAttribute("constraint")==null && ans.getAttribute("constraint")==null){
								structexactmatch++;
								structperfmatch++;
								exact += test.getAttributeValue("id");
								ansexact += ans.getAttributeValue("id");
								break;
							}
						}
					}
				}
			}
		}
		for(int i = 0; i < testli.size(); i++){
			Element test = (Element)testli.get(i);
			if(!exact.contains(test.getAttributeValue("id"))){
				for(int j = 0; j < ansli.size(); j++){
					Element ans = (Element)ansli.get(j);
					if(!ansexact.contains(ans.getAttributeValue("id"))){
						if (test.getAttributeValue("name").compareTo(ans.getAttributeValue("name"))==0){
							if (test.getAttribute("constraint")!=null && ans.getAttribute("constraint")!=null){
								if (test.getAttributeValue("constraint").contains(ans.getAttributeValue("constraint"))|ans.getAttributeValue("constraint").contains(test.getAttributeValue("constraint"))){
									structpartmatch++;
									exact += test.getAttributeValue("id");
									ansexact += ans.getAttributeValue("id");
									break;
								}
							}
						}
					}
				}
			}
		}
		for(int i = 0; i < testli.size(); i++){
			Element test = (Element)testli.get(i);
			if(!exact.contains(test.getAttributeValue("id"))){
				for(int j = 0; j < ansli.size(); j++){
					Element ans = (Element)ansli.get(j);
					if(!ansexact.contains(ans.getAttributeValue("id"))){
						if (ans.getAttributeValue("name").contains(test.getAttributeValue("name"))|test.getAttributeValue("name").contains(ans.getAttributeValue("name"))){
							if (test.getAttribute("constraint")!=null && ans.getAttribute("constraint")!=null){
								if (test.getAttributeValue("constraint").contains(ans.getAttributeValue("constraint"))|ans.getAttributeValue("constraint").contains(test.getAttributeValue("constraint"))){
									structpartmatch++;
									exact += test.getAttributeValue("id");
									ansexact += ans.getAttributeValue("id");
									break;
								}
							}
							else{
								if(test.getAttribute("constraint")==null && ans.getAttribute("constraint")==null){
									structpartmatch++;
									exact += test.getAttributeValue("id");
									ansexact += ans.getAttributeValue("id");
									break;
								}
							}
						}
					}
				}
			}
		}
	/*	System.out.println("structperfmatch:"+structperfmatch);
		System.out.println("structexactmatch:"+structexactmatch);
		System.out.println("structpartmatch:"+structpartmatch);
		System.out.println("structnomatch:"+structnomatch);*/
	}
	
	/**
	 * Performs comparison between the <character> elements present in machine & human annotated sentence.
	 * @param ansroot
	 * @param testroot
	 */
	public void validatecharacter(Element ansroot, Element testroot) {
		String exact = "";
		String ansliexact = "";
		ArrayList<List> myansli = new ArrayList();
		ArrayList<List> mytestli = new ArrayList();
		List ansli = null, testli = null;
		List structansli = ansroot.getChildren("structure");
		List structtestli = testroot.getChildren("structure");
		int flags = 0;
		for( Iterator l = structansli.iterator(); l.hasNext();){
			Element structans = (Element)l.next();
			if(!structans.getChildren().isEmpty()){
					ansli = structans.getChildren();
					myansli.add(ansli);
					tothumanch += ansli.size();
			}
		}
		flags = 0;
		for( Iterator l = structtestli.iterator(); l.hasNext();){
			Element structtest = (Element)l.next();
			if(!structtest.getChildren().isEmpty()){
					testli = structtest.getChildren();
					mytestli.add(testli);
					totmachinech += testli.size();
			}
		}

		if(myansli!=null && mytestli!=null){
			for(Iterator tli = mytestli.iterator(); tli.hasNext();){
				testli = (List)tli.next();
				for(int i = 0; i < testli.size(); i++){
					int flag = 0;
					Element test = (Element)testli.get(i);
					for(Iterator ali = myansli.iterator(); ali.hasNext();){
						int aliflag = 0;
						ansli = (List)ali.next();
						for(int j = 0; j < ansli.size(); j++){
							flag = 0;
							Element ans = (Element)ansli.get(j);
							//System.out.println(test.getParentElement().getAttributeValue("id"));
							//System.out.println(ans.getParentElement().getAttributeValue("id"));
							if (test.getParentElement().getAttributeValue("name").compareTo(ans.getParentElement().getAttributeValue("name"))==0){
								List testattr = test.getAttributes();
								List ansattr = ans.getAttributes();
								for(Iterator k = testattr.iterator(); k.hasNext();){
									Attribute a = (Attribute)k.next();
									//System.out.println(ansattr.toString());
									//System.out.println(a.toString());
									if(a.getName().compareTo("name")==0|a.getName().compareTo("value")==0|a.getName().compareTo("char_type")==0|a.getName().compareTo("modifier")==0|a.getName().compareTo("from")==0|a.getName().compareTo("to")==0|a.getName().compareTo("from_unit")==0|a.getName().compareTo("to_unit")==0|a.getName().compareTo("unit")==0){
										if(!ansattr.toString().contains(a.toString())){
											flag = 1;
											break;
										}
									}
								}
								if(flag == 0){
									charexactmatch++;
									if(test.getAttributes().size() == ans.getAttributes().size())
										charperfmatch++;
									exact += test.getAttributes().toString();
									ansliexact += ans.getAttributes().toString();
									aliflag = 1;
									break;
								}
							}
						}
						if(aliflag == 1)
							break;
					}
				}
			}
			
			for(Iterator tli = mytestli.iterator(); tli.hasNext();){
				testli = (List)tli.next();			
				for(int i = 0; i < testli.size(); i++){
					int flag = 0;
					Element test = (Element)testli.get(i);
					if(!exact.contains(test.getAttributes().toString())){
						for(Iterator ali = myansli.iterator(); ali.hasNext();){
							int aliflag = 0;
							ansli = (List)ali.next();
							for(int j = 0; j < ansli.size(); j++){
								int ansliflag = 0;
								Element ans = (Element)ansli.get(j);
								if(!ansliexact.contains(ans.getAttributes().toString())){
									if (test.getParentElement().getAttributeValue("name").compareTo(ans.getParentElement().getAttributeValue("name"))==0){
										int count = 0, passct = 0;
										List testattr = test.getAttributes();
										List ansattr = ans.getAttributes();
										int missattr = 0;
										for(int k = 0; k < testattr.size(); k++){
											Attribute atest = (Attribute)testattr.get(k);
											if(atest.getName().compareTo("name")==0|atest.getName().compareTo("value")==0|atest.getName().compareTo("char_type")==0|atest.getName().compareTo("modifier")==0|atest.getName().compareTo("from")==0|atest.getName().compareTo("to")==0|atest.getName().compareTo("from_unit")==0|atest.getName().compareTo("to_unit")==0|atest.getName().compareTo("unit")==0){	
												missattr = 0;
												for(int l = 0; l < ansattr.size(); l++){
													Attribute aans = (Attribute)ansattr.get(l);
													if(atest.getName().toString().compareTo(aans.getName().toString())==0){
														missattr = 1;
														if(aans.getValue().contains(atest.getValue())|atest.getValue().contains(aans.getValue())){
															break;
														}
														else{
															ansliflag = 1;
															break;
														}
													}
												}
												if(missattr == 0)
													break;
											}
											if(ansliflag == 1)
												break;
										}
										if(ansliflag == 0 && missattr == 1){
											charpartmatch++;
											ansliexact += ans.getAttributes().toString();
											flag = 1;
											aliflag = 1;
											break;
										}
									}
								}
							}
							if (aliflag == 1){
								break;
							}
						}
						if (flag == 0){
							charnomatch++;
						}
					}
				}
			}
		/*	System.out.println("charperfmatch:"+charperfmatch);
			System.out.println("charexactmatch:"+charexactmatch);
			System.out.println("charpartmatch:"+charpartmatch);
			System.out.println("charnomatch:"+charnomatch);*/
		}
	}
	
	/**
	 * Performs comparison between the <relation> elements present in machine & human annotated sentence.
	 * @param ansroot
	 * @param testroot
	 */
	public void validaterelation(Element ansroot, Element testroot) {
		String exact = "";
		List ansli = ansroot.getChildren("relation");
		List testli = testroot.getChildren("relation");
		totmachinerel = testli.size();
		tothumanrel = ansli.size();
		for(int i = 0; i < testli.size(); i++){
			int flag = 0;
			Element test = (Element)testli.get(i);
			for(int j = 0; j < ansli.size(); j++){
				flag = 0;
				Element ans = (Element)ansli.get(j);
				List testattr = test.getAttributes();
				List ansattr = ans.getAttributes();
				for(int k = 0; k < testattr.size(); k++){
					Attribute a = (Attribute)testattr.get(k);
					//System.out.println(ansattr.toString());
					//System.out.println(a.toString());
					if(a.getName().compareTo("name")==0|a.getName().compareTo("from")==0|a.getName().compareTo("to")==0|a.getName().compareTo("negation")==0){
						if(a.getName().compareTo("from")==0|a.getName().compareTo("to")==0){
							List ansstruct = ansroot.getChildren("structure");
							List teststruct = testroot.getChildren("structure");
							String teststname = "";
							for(int m = 0; m < teststruct.size(); m++){
								Element testst = (Element)teststruct.get(m);
								if(testst.getAttributeValue("id").compareTo(a.getValue())==0){
									teststname = testst.getAttributeValue("name");
									break;
								}
							}
							String aansval = "";
							for(int n = 0; n < ansattr.size(); n++){
								Attribute aans = (Attribute)ansattr.get(n); 
								if(aans.getName().compareTo(a.getName())==0){
									aansval = aans.getValue();
									break;
								}
							}
							String ansstname = "";
							for(int m = 0; m < ansstruct.size(); m++){
								Element ansst = (Element)ansstruct.get(m);
								if(ansst.getAttributeValue("id").compareTo(aansval)==0){
									ansstname = ansst.getAttributeValue("name");
									break;
								}
							}
							if(!teststname.contains(ansstname) && !ansstname.contains(teststname)){
								flag = 1;
								break;
							}
						}
						else if(!ansattr.toString().contains(a.toString())){
							flag = 1;
							break;
						}
					}
				}
				if(flag == 0){
					relexactmatch++;
					if(test.getAttributes().size() == ans.getAttributes().size())
						relperfmatch++;
					exact += test.getAttributeValue("id");
					ansli.remove(j);
					break;
				}
			}
		}
		
		for(int i = 0; i < testli.size(); i++){
			Element test = (Element)testli.get(i);
			int testflag = 0;
			if(!exact.contains(test.getAttributeValue("id"))){
				for(int j = 0; j < ansli.size(); j++){
					int flag = 0;
					Element ans = (Element)ansli.get(j);
					List testattr = test.getAttributes();
					List ansattr = ans.getAttributes();
					for(int k = 0; k < testattr.size(); k++){
						Attribute atest = (Attribute)testattr.get(k);
						if(atest.getName().compareTo("name")==0|atest.getName().compareTo("from")==0|atest.getName().compareTo("to")==0|atest.getName().compareTo("negation")==0){	
							for(int l = 0; l < ansattr.size(); l++){
								Attribute aans = (Attribute)ansattr.get(l);
								if(atest.getName().toString().compareTo(aans.getName().toString())==0){
									if(atest.getName().compareTo("from")==0|atest.getName().compareTo("to")==0){
										List ansstruct = ansroot.getChildren("structure");
										List teststruct = testroot.getChildren("structure");
										String teststname = "";
										for(int m = 0; m < teststruct.size(); m++){
											Element testst = (Element)teststruct.get(m);
											if(testst.getAttributeValue("id").compareTo(atest.getValue())==0){
												teststname = testst.getAttributeValue("name");
												break;
											}
										}
										String aansval = "";
										for(int n = 0; n < ansattr.size(); n++){
											Attribute aansk = (Attribute)ansattr.get(n); 
											if(aansk.getName().compareTo(atest.getName())==0){
												aansval = aansk.getValue();
												break;
											}
										}
										String ansstname = "";
										for(int m = 0; m < ansstruct.size(); m++){
											Element ansst = (Element)ansstruct.get(m);
											if(ansst.getAttributeValue("id").compareTo(aansval)==0){
												ansstname = ansst.getAttributeValue("name");
												break;
											}
										}
										if(teststname.contains(ansstname)|ansstname.contains(teststname)){
											break;
										}
										else{
											flag = 1;
											break;
										}
									}
									else{
										if(aans.getValue().contains(atest.getValue())|atest.getValue().contains(aans.getValue())){
											break;
										}
										else{
											flag = 1;
											break;
										}
									}
								}
							}
						}
						if(flag == 1){
							break;
						}
					}
					if(flag == 0){
						relpartmatch++;
						exact += test.getAttributeValue("id");
						ansli.remove(j);
						testflag = 1;
						break;
					}
				}
				if(testflag == 0){
					relnomatch++;
				}
			}
		}
	/*	System.out.println("relperfmatch:"+relperfmatch);
		System.out.println("relexactmatch:"+relexactmatch);
		System.out.println("relpartmatch:"+relpartmatch);
		System.out.println("relnomatch:"+relnomatch);*/
	}
	
	/**
	 * Performs Precision & Recall calculations for Perfect match, exact match, partial match, reasonable match
	 * @param source
	 */
	public void calcprecisionrecall(String source){
		try{
			float pperfst, pexactst, ppartialst, preasonst;
			float pperfch, pexactch, ppartialch, preasonch;
			float pperfrel, pexactrel, ppartialrel, preasonrel;
			float rperfst, rexactst, rpartialst, rreasonst;
			float rperfch, rexactch, rpartialch, rreasonch;
			float rperfrel, rexactrel, rpartialrel, rreasonrel;
			float sentprecision, sentrecall;
						
			Statement stmt = conn.createStatement();
			
			pperfst = (float)structperfmatch/totmachinest;
			pexactst = (float)structexactmatch/totmachinest;
			ppartialst = (float)structpartmatch/totmachinest;
			preasonst = (float)(structexactmatch+structpartmatch)/totmachinest;
						
			if(totmachinech == 0){
				pperfch = 0;
				pexactch = 0;
				ppartialch = 0;
				preasonch = 0;
			}
			else{
				pperfch = (float)charperfmatch/totmachinech;
				pexactch = (float)charexactmatch/totmachinech;
				ppartialch = (float)charpartmatch/totmachinech;
				preasonch = (float)(charexactmatch+charpartmatch)/totmachinech;
			}
			
			if(totmachinerel == 0){
				pperfrel = 0;
				pexactrel = 0;
				ppartialrel = 0;
				preasonrel = 0;
			}
			else{
				pperfrel = (float)relperfmatch/totmachinerel;
				pexactrel = (float)relexactmatch/totmachinerel;
				ppartialrel = (float)relpartmatch/totmachinerel;
				preasonrel = (float)(relexactmatch+relpartmatch)/totmachinerel;
			}
			
			rperfst = (float)structperfmatch/tothumanst;
			rexactst = (float)structexactmatch/tothumanst;
			rpartialst = (float)structpartmatch/tothumanst;
			rreasonst = (float)(structexactmatch+structpartmatch)/tothumanst;
			
			if(tothumanch == 0){
				rperfch = 0;
				rexactch = 0;
				rpartialch = 0;
				rreasonch = 0;
			}
			else{
				rperfch = (float)charperfmatch/tothumanch;
				rexactch = (float)charexactmatch/tothumanch;
				rpartialch = (float)charpartmatch/tothumanch;
				rreasonch = (float)(charexactmatch+charpartmatch)/tothumanch;
			}
			
			if(tothumanrel == 0){
				rperfrel = 0;
				rexactrel = 0;
				rpartialrel = 0;
				rreasonrel = 0;
			}
			else{
				rperfrel = (float)relperfmatch/tothumanrel;
				rexactrel = (float)relexactmatch/tothumanrel;
				rpartialrel = (float)relpartmatch/tothumanrel;
				rreasonrel = (float)(relexactmatch+relpartmatch)/tothumanrel;
			}
	
			sentprecision = (float)(structexactmatch+structpartmatch+charexactmatch+charpartmatch+relexactmatch+relpartmatch)/(totmachinest+totmachinech+totmachinerel);
			sentrecall = (float)(structexactmatch+structpartmatch+charexactmatch+charpartmatch+relexactmatch+relpartmatch)/(tothumanst+tothumanch+tothumanrel);
			
			stmt.execute("insert into precisionrecall values('"+source+"','"+pperfst+"','"+pexactst+"','"+ppartialst+"','"+preasonst+"','"+pperfch+"','"+pexactch+"','"+ppartialch+"','"+preasonch+"','"+pperfrel+"','"+pexactrel+"','"+ppartialrel+"','"+preasonrel+"'," +
					"'"+rperfst+"','"+rexactst+"','"+rpartialst+"','"+rreasonst+"','"+rperfch+"','"+rexactch+"','"+rpartialch+"','"+rreasonch+"','"+rperfrel+"','"+rexactrel+"','"+rpartialrel+"','"+rreasonrel+"','"+sentprecision+"','"+sentrecall+"')");
			
			dscstructexactmatch += structexactmatch;
			dscstructpartmatch += structpartmatch;
			dscstructnomatch += structnomatch;
			dscstructperfmatch += structperfmatch;
			dsccharexactmatch += charexactmatch;
			dsccharpartmatch += charpartmatch;
			dsccharnomatch += charnomatch;
			dsccharperfmatch += charperfmatch;
			dscrelexactmatch += relexactmatch;
			dscrelpartmatch += relpartmatch;
			dscrelnomatch += relnomatch;
			dscrelperfmatch += relperfmatch;
			dsctotmachinest += totmachinest;
			dsctotmachinech += totmachinech;
			dsctotmachinerel += totmachinerel;
			dsctothumanst += tothumanst;
			dsctothumanch += tothumanch;
			dsctothumanrel += tothumanrel;
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * Performs Precision & Recall calculations for Perfect match, exact match, partial match, reasonable match
	 * of the entire description
	 * @param source
	 */
	public void dsccalcprecisionrecall(){
		try{
			Statement stmt = conn.createStatement();
			
			float dscpperfst, dscpexactst, dscppartialst, dscpreasonst;
			float dscpperfch, dscpexactch, dscppartialch, dscpreasonch;
			float dscpperfrel, dscpexactrel, dscppartialrel, dscpreasonrel;
			float dscrperfst, dscrexactst, dscrpartialst, dscrreasonst;
			float dscrperfch, dscrexactch, dscrpartialch, dscrreasonch;
			float dscrperfrel, dscrexactrel, dscrpartialrel, dscrreasonrel;
			
			dscpperfst = (float)dscstructperfmatch/dsctotmachinest;
			dscpexactst = (float)dscstructexactmatch/dsctotmachinest;
			dscppartialst = (float)dscstructpartmatch/dsctotmachinest;
			dscpreasonst = (float)(dscstructexactmatch+dscstructpartmatch)/dsctotmachinest;
						
			dscpperfch = (float)dsccharperfmatch/dsctotmachinech;
			dscpexactch = (float)dsccharexactmatch/dsctotmachinech;
			dscppartialch = (float)dsccharpartmatch/dsctotmachinech;
			dscpreasonch = (float)(dsccharexactmatch+dsccharpartmatch)/dsctotmachinech;
			
			dscpperfrel = (float)dscrelperfmatch/dsctotmachinerel;
			dscpexactrel = (float)dscrelexactmatch/dsctotmachinerel;
			dscppartialrel = (float)dscrelpartmatch/dsctotmachinerel;
			dscpreasonrel = (float)(dscrelexactmatch+dscrelpartmatch)/dsctotmachinerel;
			
			dscrperfst = (float)dscstructperfmatch/dsctothumanst;
			dscrexactst = (float)dscstructexactmatch/dsctothumanst;
			dscrpartialst = (float)dscstructpartmatch/dsctothumanst;
			dscrreasonst = (float)(dscstructexactmatch+dscstructpartmatch)/dsctothumanst;
			
			dscrperfch = (float)dsccharperfmatch/dsctothumanch;
			dscrexactch = (float)dsccharexactmatch/dsctothumanch;
			dscrpartialch = (float)dsccharpartmatch/dsctothumanch;
			dscrreasonch = (float)(dsccharexactmatch+dsccharpartmatch)/dsctothumanch;
			
			dscrperfrel = (float)dscrelperfmatch/dsctothumanrel;
			dscrexactrel = (float)dscrelexactmatch/dsctothumanrel;
			dscrpartialrel = (float)dscrelpartmatch/dsctothumanrel;
			dscrreasonrel = (float)(dscrelexactmatch+dscrelpartmatch)/dsctothumanrel;
			System.out.println( "dscpperfst "+dscpperfst+"\n"+"dscpexactst "+dscpexactst+"\n"+"dscppatialst "+ dscppartialst+"\n"+"dscpreasonst "+dscpreasonst+"\n"+
					"dscpperfch "+dscpperfch+"\n"+"dscpexactch "+ dscpexactch+"\n"+"dscppartialch "+ dscppartialch+"\n"+"dscpreasonch "+ dscpreasonch+"\n"+
					"dscpperfrel "+dscpperfrel+"\n"+"dscpexactrel "+ dscpexactrel+"\n"+"dscppartialrel "+dscppartialrel+"\n"+"dscpreasonrel "+ dscpreasonrel+"\n"+
					"dscrperfst "+dscrperfst+"\n"+ "dscrexactst "+dscrexactst+"\n"+ "dscrpartialst "+dscrpartialst+"\n"+ "dscrreasonst "+dscrreasonst+"\n"+
					"dscrperfch "+dscrperfch+"\n"+ "dscrexactch "+dscrexactch+"\n"+ "dscrpartialch "+dscrpartialch+"\n"+ "dscrreasonch "+dscrreasonch+"\n"+
					"dscrperfrel "+dscrperfrel+"\n"+ "dscrexactrel "+dscrexactrel+"\n"+ "dscrpartialrel "+dscrpartialrel+"\n"+ "dscrreasonrel "+dscrreasonrel);
			
			stmt.execute("insert into precisionrecall values('desc','"+dscpperfst+"','"+dscpexactst+"','"+dscppartialst+"','"+dscpreasonst+"','"+dscpperfch+"','"+dscpexactch+"','"+dscppartialch+"','"+dscpreasonch+"','"+dscpperfrel+"','"+dscpexactrel+"','"+dscppartialrel+"','"+dscpreasonrel+"'," +
					"'"+dscrperfst+"','"+dscrexactst+"','"+dscrpartialst+"','"+dscrreasonst+"','"+dscrperfch+"','"+dscrexactch+"','"+dscrpartialch+"','"+dscrreasonch+"','"+dscrperfrel+"','"+dscrexactrel+"','"+dscrpartialrel+"','"+dscrreasonrel+"','0','0')");
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	
	/**
	 * 
	 * Each sentence to be compared needs to be in an individual file named by its 'source id'. The filename should 
	 * not contain any '[' or ']'. 
	 * The root element for the file should be <statement>.
	 * The machine generated files should be in a separate folder whose URL is stored in var:testdirectory.
	 * The human generated files should be in a separate folder whose URL is stored in var:ansdirectory.
	 *  
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CompareXML cXML = new CompareXML();
	}

}
