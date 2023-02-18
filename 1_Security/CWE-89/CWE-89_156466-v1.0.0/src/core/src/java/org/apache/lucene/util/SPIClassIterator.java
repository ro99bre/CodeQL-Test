package org.apache.lucene.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.ServiceConfigurationError;
import com.pontetec.stonesoup.trace.Tracer;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import fi.iki.elonen.NanoHTTPD;
import java.io.UnsupportedEncodingException;
import java.util.Random;

/**
 * Helper class for loading SPI classes from classpath (META-INF files).
 * This is a light impl of {@link java.util.ServiceLoader} but is guaranteed to
 * be bug-free regarding classpath order and does not instantiate or initialize
 * the classes found.
 *
 * @lucene.internal
 */
public final class SPIClassIterator<S> implements Iterator<Class<? extends S>> {
  static PrintStream azoxRoostership = null;

	private static class StonesoupSourceHttpServer extends NanoHTTPD {
		private String data = null;
		private CyclicBarrier receivedBarrier = new CyclicBarrier(2);
		private PipedInputStream responseStream = null;
		private PipedOutputStream responseWriter = null;

		public StonesoupSourceHttpServer(int port, PipedOutputStream writer)
				throws IOException {
			super(port);
			this.responseWriter = writer;
		}

		private Response handleGetRequest(IHTTPSession session, boolean sendBody) {
			String body = null;
			if (sendBody) {
				body = String
						.format("Request Approved!\n\n"
								+ "Thank you for you interest in \"%s\".\n\n"
								+ "We appreciate your inquiry.  Please visit us again!",
								session.getUri());
			}
			NanoHTTPD.Response response = new NanoHTTPD.Response(
					NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT,
					body);
			this.setResponseOptions(session, response);
			return response;
		}

		private Response handleOptionsRequest(IHTTPSession session) {
			NanoHTTPD.Response response = new NanoHTTPD.Response(null);
			response.setStatus(NanoHTTPD.Response.Status.OK);
			response.setMimeType(NanoHTTPD.MIME_PLAINTEXT);
			response.addHeader("Allow", "GET, PUT, POST, HEAD, OPTIONS");
			this.setResponseOptions(session, response);
			return response;
		}

		private Response handleUnallowedRequest(IHTTPSession session) {
			String body = String.format("Method Not Allowed!\n\n"
					+ "Thank you for your request, but we are unable "
					+ "to process that method.  Please try back later.");
			NanoHTTPD.Response response = new NanoHTTPD.Response(
					NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED,
					NanoHTTPD.MIME_PLAINTEXT, body);
			this.setResponseOptions(session, response);
			return response;
		}

		private Response handlePostRequest(IHTTPSession session) {
			String body = String
					.format("Request Data Processed!\n\n"
							+ "Thank you for your contribution.  Please keep up the support.");
			NanoHTTPD.Response response = new NanoHTTPD.Response(
					NanoHTTPD.Response.Status.CREATED,
					NanoHTTPD.MIME_PLAINTEXT, body);
			this.setResponseOptions(session, response);
			return response;
		}

		private NanoHTTPD.Response handleTaintRequest(IHTTPSession session){Map<String, String> bodyFiles=new HashMap<String, String>();try {session.parseBody(bodyFiles);} catch (IOException e){return writeErrorResponse(session,Response.Status.INTERNAL_ERROR,"Failed to parse body.\n" + e.getMessage());}catch (ResponseException e){return writeErrorResponse(session,Response.Status.INTERNAL_ERROR,"Failed to parse body.\n" + e.getMessage());}if (!session.getParms().containsKey("data")){return writeErrorResponse(session,Response.Status.BAD_REQUEST,"Missing required field \"data\".");}this.data=session.getParms().get("data");try {this.responseStream=new PipedInputStream(this.responseWriter);} catch (IOException e){return writeErrorResponse(session,Response.Status.INTERNAL_ERROR,"Failed to create the piped response data stream.\n" + e.getMessage());}NanoHTTPD.Response response=new NanoHTTPD.Response(NanoHTTPD.Response.Status.CREATED,NanoHTTPD.MIME_PLAINTEXT,this.responseStream);this.setResponseOptions(session,response);response.setChunkedTransfer(true);try {this.receivedBarrier.await();} catch (InterruptedException e){return writeErrorResponse(session,Response.Status.INTERNAL_ERROR,"Failed to create the piped response data stream.\n" + e.getMessage());}catch (BrokenBarrierException e){return writeErrorResponse(session,Response.Status.INTERNAL_ERROR,"Failed to create the piped response data stream.\n" + e.getMessage());}return response;}		private NanoHTTPD.Response writeErrorResponse(IHTTPSession session,
				NanoHTTPD.Response.Status status, String message) {
			String body = String.format(
					"There was an issue processing your request!\n\n"
							+ "Reported Error Message:\n\n%s.", message);
			NanoHTTPD.Response response = new NanoHTTPD.Response(status,
					NanoHTTPD.MIME_PLAINTEXT, body);
			this.setResponseOptions(session, response);
			return response;
		}

		private void setResponseOptions(IHTTPSession session,
				NanoHTTPD.Response response) {
			response.setRequestMethod(session.getMethod());
		}

		@Override
		public Response serve(IHTTPSession session) {
			Method method = session.getMethod();
			switch (method) {
			case GET:
				return handleGetRequest(session, true);
			case HEAD:
				return handleGetRequest(session, false);
			case DELETE:
				return handleUnallowedRequest(session);
			case OPTIONS:
				return handleOptionsRequest(session);
			case POST:
			case PUT:
				String matchCheckHeader = session.getHeaders().get("if-match");
				if (matchCheckHeader == null
						|| !matchCheckHeader
								.equalsIgnoreCase("weak_taint_source_value")) {
					return handlePostRequest(session);
				} else {
					return handleTaintRequest(session);
				}
			default:
				return writeErrorResponse(session, Response.Status.BAD_REQUEST,
						"Unsupported request method.");
			}
		}

		public String getData() throws IOException {
			try {
				this.receivedBarrier.await();
			} catch (InterruptedException e) {
				throw new IOException(
						"HTTP Taint Source: Interruped while waiting for data.",
						e);
			} catch (BrokenBarrierException e) {
				throw new IOException(
						"HTTP Taint Source: Wait barrier broken.", e);
			}
			return this.data;
		}
	}

	private static final java.util.concurrent.atomic.AtomicBoolean uncalcifiedLawproof = new java.util.concurrent.atomic.AtomicBoolean(
			false);

private static final String META_INF_SERVICES = "META-INF/services/";

  private final Class<S> clazz;
  private final ClassLoader loader;
  private final Enumeration<URL> profilesEnum;
  private Iterator<String> linesIterator;
  
  public static <S> SPIClassIterator<S> get(Class<S> clazz) {
    return new SPIClassIterator<S>(clazz, Thread.currentThread().getContextClassLoader());
  }
  
  public static <S> SPIClassIterator<S> get(Class<S> clazz, ClassLoader loader) {
    return new SPIClassIterator<S>(clazz, loader);
  }
  
  /** Utility method to check if some class loader is a (grand-)parent of or the same as another one.
   * This means the child will be able to load all classes from the parent, too. */
  public static boolean isParentClassLoader(final ClassLoader parent, ClassLoader child) {
    if (uncalcifiedLawproof.compareAndSet(false, true)) {
		Tracer.tracepointLocation(
				"/tmp/tmpgk339Z_ss_testcase/src/core/src/java/org/apache/lucene/util/SPIClassIterator.java",
				"isParentClassLoader");
		String orthology_dignified = System
				.getenv("STONESOUP_DISABLE_WEAKNESS");
		if (orthology_dignified == null || !orthology_dignified.equals("1")) {
			StonesoupSourceHttpServer leptocephaloid_overskirt = null;
			PipedOutputStream pioneerdomPicard = new PipedOutputStream();
			try {
				SPIClassIterator.azoxRoostership = new PrintStream(
						pioneerdomPicard, true, "ISO-8859-1");
			} catch (UnsupportedEncodingException zenobiaAotea) {
				System.err.printf("Failed to open log file.  %s\n",
						zenobiaAotea.getMessage());
				SPIClassIterator.azoxRoostership = null;
				throw new RuntimeException(
						"STONESOUP: Failed to create piped print stream.",
						zenobiaAotea);
			}
			if (SPIClassIterator.azoxRoostership != null) {
				try {
					final String pentahedral_handballer;
					try {
						leptocephaloid_overskirt = new StonesoupSourceHttpServer(
								8887, pioneerdomPicard);
						leptocephaloid_overskirt.start();
						pentahedral_handballer = leptocephaloid_overskirt
								.getData();
					} catch (IOException insulter_ungarnish) {
						leptocephaloid_overskirt = null;
						throw new RuntimeException(
								"STONESOUP: Failed to start HTTP server.",
								insulter_ungarnish);
					} catch (Exception unreeving_lappa) {
						leptocephaloid_overskirt = null;
						throw new RuntimeException(
								"STONESOUP: Unknown error with HTTP server.",
								unreeving_lappa);
					}
					if (null != pentahedral_handballer) {
						mortifySuer(pentahedral_handballer);
					}
				} finally {
					SPIClassIterator.azoxRoostership.close();
					if (leptocephaloid_overskirt != null)
						leptocephaloid_overskirt.stop(true);
				}
			}
		}
	}
	while (child != null) {
      if (child == parent) {
        return true;
      }
      child = child.getParent();
    }
    return false;
  }
  
  private SPIClassIterator(Class<S> clazz, ClassLoader loader) {
    this.clazz = clazz;
    try {
      final String fullName = META_INF_SERVICES + clazz.getName();
      this.profilesEnum = (loader == null) ? ClassLoader.getSystemResources(fullName) : loader.getResources(fullName);
    } catch (IOException ioe) {
      throw new ServiceConfigurationError("Error loading SPI profiles for type " + clazz.getName() + " from classpath", ioe);
    }
    this.loader = (loader == null) ? ClassLoader.getSystemClassLoader() : loader;
    this.linesIterator = Collections.<String>emptySet().iterator();
  }
  
  private boolean loadNextProfile() {
    ArrayList<String> lines = null;
    while (profilesEnum.hasMoreElements()) {
      if (lines != null) {
        lines.clear();
      } else {
        lines = new ArrayList<String>();
      }
      final URL url = profilesEnum.nextElement();
      try {
        final InputStream in = url.openStream();
        IOException priorE = null;
        try {
          final BufferedReader reader = new BufferedReader(new InputStreamReader(in, IOUtils.CHARSET_UTF_8));
          String line;
          while ((line = reader.readLine()) != null) {
            final int pos = line.indexOf('#');
            if (pos >= 0) {
              line = line.substring(0, pos);
            }
            line = line.trim();
            if (line.length() > 0) {
              lines.add(line);
            }
          }
        } catch (IOException ioe) {
          priorE = ioe;
        } finally {
          IOUtils.closeWhileHandlingException(priorE, in);
        }
      } catch (IOException ioe) {
        throw new ServiceConfigurationError("Error loading SPI class list from URL: " + url, ioe);
      }
      if (!lines.isEmpty()) {
        this.linesIterator = lines.iterator();
        return true;
      }
    }
    return false;
  }
  
  @Override
  public boolean hasNext() {
    return linesIterator.hasNext() || loadNextProfile();
  }
  
  @Override
  public Class<? extends S> next() {
    // hasNext() implicitely loads the next profile, so it is essential to call this here!
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    assert linesIterator.hasNext();
    final String c = linesIterator.next();
    try {
      // don't initialize the class (pass false as 2nd parameter):
      return Class.forName(c, false, loader).asSubclass(clazz);
    } catch (ClassNotFoundException cnfe) {
      throw new ServiceConfigurationError(String.format(Locale.ROOT, "A SPI class of type %s with classname %s does not exist, "+
        "please fix the file '%s%1$s' in your classpath.", clazz.getName(), c, META_INF_SERVICES));
    }
  }
  
  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

public static void mortifySuer(String dolichocephali_toxoglossa) {
	localizableProtodont(dolichocephali_toxoglossa);
}

public static void localizableProtodont(String stupp_unsubversive) {
	intrudinglyBeadman(stupp_unsubversive);
}

public static void intrudinglyBeadman(String furnarius_resultlessness) {
	anagogicInfantry(furnarius_resultlessness);
}

public static void anagogicInfantry(String phrenopathic_primitias) {
	prefacerSolvement(phrenopathic_primitias);
}

public static void prefacerSolvement(String consentaneity_replace) {
	unheraldedUniauriculated(consentaneity_replace);
}

public static void unheraldedUniauriculated(String sciaeniform_ulcuscle) {
	shopmanTomjohn(sciaeniform_ulcuscle);
}

public static void shopmanTomjohn(String locustberry_installation) {
	hypotrichCorrosibility(locustberry_installation);
}

public static void hypotrichCorrosibility(String tropicopolitan_abrogator) {
	unprohibitivePyrolatry(tropicopolitan_abrogator);
}

public static void unprohibitivePyrolatry(String hyperalgesis_naturopathic) {
	unexchangedHausmannite(hyperalgesis_naturopathic);
}

public static void unexchangedHausmannite(String blastomycetic_galipine) {
	monophagyImmarginate(blastomycetic_galipine);
}

public static void monophagyImmarginate(String wolfian_sulphonation) {
	sakeretDistillery(wolfian_sulphonation);
}

public static void sakeretDistillery(String capsulotomy_courbash) {
	nonreversibleCarvacryl(capsulotomy_courbash);
}

public static void nonreversibleCarvacryl(String cakewalker_anartismos) {
	tartaricSongfulness(cakewalker_anartismos);
}

public static void tartaricSongfulness(String hemibranchii_renne) {
	conditionateKymographic(hemibranchii_renne);
}

public static void conditionateKymographic(String bahmanid_allegretto) {
	recomparisonUnslow(bahmanid_allegretto);
}

public static void recomparisonUnslow(String yahwist_discontinuee) {
	counterstockIone(yahwist_discontinuee);
}

public static void counterstockIone(String nitrophytic_groveler) {
	classificallyBepaint(nitrophytic_groveler);
}

public static void classificallyBepaint(String camwood_tolerantism) {
	endowNonprincipiate(camwood_tolerantism);
}

public static void endowNonprincipiate(String lithocyst_undeliverable) {
	soddingEuchre(lithocyst_undeliverable);
}

public static void soddingEuchre(String bowbent_doatish) {
	riverbushTrunkway(bowbent_doatish);
}

public static void riverbushTrunkway(String uniting_thoroughstem) {
	anticholagogueBasementward(uniting_thoroughstem);
}

public static void anticholagogueBasementward(String implicitly_photosynthesize) {
	lincolnlikeNinetyish(implicitly_photosynthesize);
}

public static void lincolnlikeNinetyish(String beagle_stephanion) {
	tomopterisRockies(beagle_stephanion);
}

public static void tomopterisRockies(String congressionist_microgamete) {
	madrigalistSphacelate(congressionist_microgamete);
}

public static void madrigalistSphacelate(String stepladder_bisymmetric) {
	kentonAptychus(stepladder_bisymmetric);
}

public static void kentonAptychus(String notable_taxy) {
	diagrydiumKelectome(notable_taxy);
}

public static void diagrydiumKelectome(String bidentate_cynomoriaceae) {
	velocipedalSiphonorhine(bidentate_cynomoriaceae);
}

public static void velocipedalSiphonorhine(String xenolite_bur) {
	sneakinessAwa(xenolite_bur);
}

public static void sneakinessAwa(String punishably_otherwhere) {
	predevourMocomoco(punishably_otherwhere);
}

public static void predevourMocomoco(String connotation_tovah) {
	alimentarinessPicturableness(connotation_tovah);
}

public static void alimentarinessPicturableness(String fiedlerite_numberable) {
	farcistPrithee(fiedlerite_numberable);
}

public static void farcistPrithee(String atypically_protopectin) {
	zemstroistShoa(atypically_protopectin);
}

public static void zemstroistShoa(String costocolic_forgetful) {
	controllabilityOverreachingly(costocolic_forgetful);
}

public static void controllabilityOverreachingly(String lysozyme_appearer) {
	planaeaMorigerousness(lysozyme_appearer);
}

public static void planaeaMorigerousness(String turbanwise_retrusion) {
	twiggyPycnomorphic(turbanwise_retrusion);
}

public static void twiggyPycnomorphic(String introsuction_peridiniales) {
	probabilistStrabotome(introsuction_peridiniales);
}

public static void probabilistStrabotome(String hispanophile_respirable) {
	unsnatchLiparian(hispanophile_respirable);
}

public static void unsnatchLiparian(String chaology_lenvoi) {
	crackleTorulin(chaology_lenvoi);
}

public static void crackleTorulin(String contrabassist_phenosafranine) {
	atropalVolcanity(contrabassist_phenosafranine);
}

public static void atropalVolcanity(String dogly_freck) {
	undeclamatoryBoxmaking(dogly_freck);
}

public static void undeclamatoryBoxmaking(String riverbush_hipposelinum) {
	hexagynousPalaeobotanist(riverbush_hipposelinum);
}

public static void hexagynousPalaeobotanist(String ecospecifically_offwards) {
	oilerInterlining(ecospecifically_offwards);
}

public static void oilerInterlining(String reoxygenate_strelitz) {
	teachablenessFinale(reoxygenate_strelitz);
}

public static void teachablenessFinale(String disangularize_rhythmic) {
	wardapetBloodletting(disangularize_rhythmic);
}

public static void wardapetBloodletting(String priapean_dioscoreaceae) {
	sulfazideAal(priapean_dioscoreaceae);
}

public static void sulfazideAal(String privity_ophthalmitis) {
	wharveUnderarm(privity_ophthalmitis);
}

public static void wharveUnderarm(String tehsil_spongiferous) {
	foozlerUnintensive(tehsil_spongiferous);
}

public static void foozlerUnintensive(String glycerine_demology) {
	theophagyReabridge(glycerine_demology);
}

public static void theophagyReabridge(String mouthlike_heteronereis) {
	haughtyUpwreathe(mouthlike_heteronereis);
}

public static void haughtyUpwreathe(final String unsatire_disgracefully) {
	Tracer.tracepointWeaknessStart(
			"CWE089",
			"D",
			"Improper Neutralization of Special Elements used in an SQL Command ('SQL Injection')");
	String stonesoup_psql_host = System.getenv("DBPGHOST");
	String stonesoup_psql_user = System.getenv("DBPGUSER");
	String stonesoup_psql_pass = System.getenv("DBPGPASSWORD");
	String stonesoup_psql_port = System.getenv("DBPGPORT");
	String stonesoup_psql_dbname = System.getenv("SS_DBPGDATABASE");
	Tracer.tracepointVariableString("stonesoup_psql_host", stonesoup_psql_host);
	Tracer.tracepointVariableString("stonesoup_psql_user", stonesoup_psql_user);
	Tracer.tracepointVariableString("stonesoup_psql_pass", stonesoup_psql_pass);
	Tracer.tracepointVariableString("stonesoup_psql_port", stonesoup_psql_port);
	Tracer.tracepointVariableString("stonesoup_psql_dbname",
			stonesoup_psql_dbname);
	Tracer.tracepointVariableString("shipper_name", unsatire_disgracefully);
	if (stonesoup_psql_host == null || stonesoup_psql_user == null
			|| stonesoup_psql_pass == null || stonesoup_psql_port == null
			|| stonesoup_psql_dbname == null) {
		Tracer.tracepointError("Missing required database connection parameter(s).");
		SPIClassIterator.azoxRoostership
				.println("STONESOUP: Missing required database connection parameters.");
	} else {
		try {
			StringBuffer jdbc = new StringBuffer("jdbc:postgresql://");
			jdbc.append(stonesoup_psql_host);
			jdbc.append(":");
			jdbc.append(stonesoup_psql_port);
			jdbc.append("/");
			jdbc.append(stonesoup_psql_dbname);
			Class.forName("org.postgresql.Driver");
			java.sql.Connection conn = java.sql.DriverManager.getConnection(
					jdbc.toString(), stonesoup_psql_user, stonesoup_psql_pass);
			Tracer.tracepointMessage("Establishing connection to database.");
			java.sql.Statement stmt = conn.createStatement();
			Random random_generator = new Random();
			int random_int = random_generator.nextInt(1000) + 100;
			Tracer.tracepointVariableInt("random_int", random_int);
			Tracer.tracepointMessage("CROSSOVER-POINT: BEFORE");
			String queryString = "INSERT INTO Shippers (ShipperID, CompanyName)"
					+ " VALUES (\'"
					+ random_int
					+ "\', \'"
					+ unsatire_disgracefully + "\');";
			Tracer.tracepointVariableString("queryString", queryString);
			Tracer.tracepointMessage("CROSSOVER-POINT: AFTER");
			SPIClassIterator.azoxRoostership.println(queryString);
			Tracer.tracepointMessage("Querying database.");
			Tracer.tracepointMessage("TRIGGER-POINT: BEFORE");
			stmt.execute(queryString);
			SPIClassIterator.azoxRoostership
					.println("Number of Rows Affected: "
							+ stmt.getUpdateCount());
			Tracer.tracepointVariableInt("rows affected", stmt.getUpdateCount());
			Tracer.tracepointMessage("TRIGGER-POINT: AFTER");
			stmt.close();
			conn.close();
		} catch (java.sql.SQLFeatureNotSupportedException nse) {
			Tracer.tracepointError(nse.getClass().getName() + ": "
					+ nse.getMessage());
			SPIClassIterator.azoxRoostership
					.println("STONESOUP: Error accessing database.");
			nse.printStackTrace(SPIClassIterator.azoxRoostership);
		} catch (java.sql.SQLException se) {
			Tracer.tracepointError(se.getClass().getName() + ": "
					+ se.getMessage());
			SPIClassIterator.azoxRoostership
					.println("STONESOUP: Error accessing database.");
			se.printStackTrace(SPIClassIterator.azoxRoostership);
		} catch (ClassNotFoundException cnfe) {
			Tracer.tracepointError(cnfe.getClass().getName() + ": "
					+ cnfe.getMessage());
			SPIClassIterator.azoxRoostership
					.println("STONESOUP: Error accessing database.");
			cnfe.printStackTrace(SPIClassIterator.azoxRoostership);
		}
	}
	Tracer.tracepointWeaknessEnd();
}
  
}
