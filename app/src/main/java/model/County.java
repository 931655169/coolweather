package model;

/**
 * Created by zjm on 2016/7/17.
 */
public class County {
    private int id;
    private String countyName;
    private String countyCode;
    private int cityId;
    public int getId(){
        return id;
    }
    public void setId(int id){
        this.id=id;
    }
    public String getCountyName(){
        return countyName;
    }
    public void setCountyName(){
        this.countyName=countyName;
    }
    public String getCountyCode(){
        return countyCode;
    }
    public void setCountyCode(String countyCode){
        this.countyCode=countyCode;
    }
    public int getCityId(){
        return cityId;
    }
    public void setCountyName(int cityId){
        this.cityId=cityId;
    }
}
