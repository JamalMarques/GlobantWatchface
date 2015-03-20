package com.example.android.wearable.watchface;

import java.util.HashMap;
import java.util.Map;
import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonPropertyOrder({
        "id",
        "t",
        "e",
        "l",
        "l_fix",
        "l_cur",
        "s",
        "ltt",
        "lt",
        "lt_dts",
        "c",
        "c_fix",
        "cp",
        "cp_fix",
        "ccol",
        "pcls_fix"
})
public class StockQuote {

    @JsonProperty("id")
    private String id;
    @JsonProperty("t")
    private String t;
    @JsonProperty("e")
    private String e;
    @JsonProperty("l")
    private String l;
    @JsonProperty("l_fix")
    private String lFix;
    @JsonProperty("l_cur")
    private String lCur;
    @JsonProperty("s")
    private String s;
    @JsonProperty("ltt")
    private String ltt;
    @JsonProperty("lt")
    private String lt;
    @JsonProperty("lt_dts")
    private String ltDts;
    @JsonProperty("c")
    private String c;
    @JsonProperty("c_fix")
    private String cFix;
    @JsonProperty("cp")
    private String cp;
    @JsonProperty("cp_fix")
    private String cpFix;
    @JsonProperty("ccol")
    private String ccol;
    @JsonProperty("pcls_fix")
    private String pclsFix;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     *
     * @return
     * The id
     */
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    /**
     *
     * @param id
     * The id
     */
    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    /**
     *
     * @return
     * The t
     */
    @JsonProperty("t")
    public String getT() {
        return t;
    }

    /**
     *
     * @param t
     * The t
     */
    @JsonProperty("t")
    public void setT(String t) {
        this.t = t;
    }

    /**
     *
     * @return
     * The e
     */
    @JsonProperty("e")
    public String getE() {
        return e;
    }

    /**
     *
     * @param e
     * The e
     */
    @JsonProperty("e")
    public void setE(String e) {
        this.e = e;
    }

    /**
     *
     * @return
     * The l
     */
    @JsonProperty("l")
    public String getL() {
        return l;
    }

    /**
     *
     * @param l
     * The l
     */
    @JsonProperty("l")
    public void setL(String l) {
        this.l = l;
    }

    /**
     *
     * @return
     * The lFix
     */
    @JsonProperty("l_fix")
    public String getLFix() {
        return lFix;
    }

    /**
     *
     * @param lFix
     * The l_fix
     */
    @JsonProperty("l_fix")
    public void setLFix(String lFix) {
        this.lFix = lFix;
    }

    /**
     *
     * @return
     * The lCur
     */
    @JsonProperty("l_cur")
    public String getLCur() {
        return lCur;
    }

    /**
     *
     * @param lCur
     * The l_cur
     */
    @JsonProperty("l_cur")
    public void setLCur(String lCur) {
        this.lCur = lCur;
    }

    /**
     *
     * @return
     * The s
     */
    @JsonProperty("s")
    public String getS() {
        return s;
    }

    /**
     *
     * @param s
     * The s
     */
    @JsonProperty("s")
    public void setS(String s) {
        this.s = s;
    }

    /**
     *
     * @return
     * The ltt
     */
    @JsonProperty("ltt")
    public String getLtt() {
        return ltt;
    }

    /**
     *
     * @param ltt
     * The ltt
     */
    @JsonProperty("ltt")
    public void setLtt(String ltt) {
        this.ltt = ltt;
    }

    /**
     *
     * @return
     * The lt
     */
    @JsonProperty("lt")
    public String getLt() {
        return lt;
    }

    /**
     *
     * @param lt
     * The lt
     */
    @JsonProperty("lt")
    public void setLt(String lt) {
        this.lt = lt;
    }

    /**
     *
     * @return
     * The ltDts
     */
    @JsonProperty("lt_dts")
    public String getLtDts() {
        return ltDts;
    }

    /**
     *
     * @param ltDts
     * The lt_dts
     */
    @JsonProperty("lt_dts")
    public void setLtDts(String ltDts) {
        this.ltDts = ltDts;
    }

    /**
     *
     * @return
     * The c
     */
    @JsonProperty("c")
    public String getC() {
        return c;
    }

    /**
     *
     * @param c
     * The c
     */
    @JsonProperty("c")
    public void setC(String c) {
        this.c = c;
    }

    /**
     *
     * @return
     * The cFix
     */
    @JsonProperty("c_fix")
    public String getCFix() {
        return cFix;
    }

    /**
     *
     * @param cFix
     * The c_fix
     */
    @JsonProperty("c_fix")
    public void setCFix(String cFix) {
        this.cFix = cFix;
    }

    /**
     *
     * @return
     * The cp
     */
    @JsonProperty("cp")
    public String getCp() {
        return cp;
    }

    /**
     *
     * @param cp
     * The cp
     */
    @JsonProperty("cp")
    public void setCp(String cp) {
        this.cp = cp;
    }

    /**
     *
     * @return
     * The cpFix
     */
    @JsonProperty("cp_fix")
    public String getCpFix() {
        return cpFix;
    }

    /**
     *
     * @param cpFix
     * The cp_fix
     */
    @JsonProperty("cp_fix")
    public void setCpFix(String cpFix) {
        this.cpFix = cpFix;
    }

    /**
     *
     * @return
     * The ccol
     */
    @JsonProperty("ccol")
    public String getCcol() {
        return ccol;
    }

    /**
     *
     * @param ccol
     * The ccol
     */
    @JsonProperty("ccol")
    public void setCcol(String ccol) {
        this.ccol = ccol;
    }

    /**
     *
     * @return
     * The pclsFix
     */
    @JsonProperty("pcls_fix")
    public String getPclsFix() {
        return pclsFix;
    }

    /**
     *
     * @param pclsFix
     * The pcls_fix
     */
    @JsonProperty("pcls_fix")
    public void setPclsFix(String pclsFix) {
        this.pclsFix = pclsFix;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}