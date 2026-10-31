LANGUAGES = [
    {"code":"en", "name":"English", "native_name":"English"}, {"code":"hi", "name":"Hindi", "native_name":"हिन्दी"},
    {"code":"bn", "name":"Bengali", "native_name":"বাংলা"}, {"code":"te", "name":"Telugu", "native_name":"తెలుగు"},
    {"code":"mr", "name":"Marathi", "native_name":"मराठी"}, {"code":"ta", "name":"Tamil", "native_name":"தமிழ்"},
    {"code":"gu", "name":"Gujarati", "native_name":"ગુજરાતી"}, {"code":"kn", "name":"Kannada", "native_name":"ಕನ್ನಡ"},
    {"code":"ml", "name":"Malayalam", "native_name":"മലയാളം"}, {"code":"pa", "name":"Punjabi", "native_name":"ਪੰਜਾਬੀ"},
]
SIGNS = [
    ("aries","Aries","Mar 21 - Apr 19","♈"),("taurus","Taurus","Apr 20 - May 20","♉"),("gemini","Gemini","May 21 - Jun 20","♊"),
    ("cancer","Cancer","Jun 21 - Jul 22","♋"),("leo","Leo","Jul 23 - Aug 22","♌"),("virgo","Virgo","Aug 23 - Sep 22","♍"),
    ("libra","Libra","Sep 23 - Oct 22","♎"),("scorpio","Scorpio","Oct 23 - Nov 21","♏"),("sagittarius","Sagittarius","Nov 22 - Dec 21","♐"),
    ("capricorn","Capricorn","Dec 22 - Jan 19","♑"),("aquarius","Aquarius","Jan 20 - Feb 18","♒"),("pisces","Pisces","Feb 19 - Mar 20","♓"),
]
SIGN_NAMES = {
    "en": ["Aries","Taurus","Gemini","Cancer","Leo","Virgo","Libra","Scorpio","Sagittarius","Capricorn","Aquarius","Pisces"],
    "hi": ["मेष","वृषभ","मिथुन","कर्क","सिंह","कन्या","तुला","वृश्चिक","धनु","मकर","कुंभ","मीन"],
    "bn": ["মেষ","বৃষ","মিথুন","কর্কট","সিংহ","কন্যা","তুলা","বৃশ্চিক","ধনু","মকর","কুম্ভ","মীন"],
    "te": ["మేషం","వృషభం","మిథునం","కర్కాటకం","సింహం","కన్య","తుల","వృశ్చికం","ధనుస్సు","మకరం","కుంభం","మీనం"],
    "mr": ["मेष","वृषभ","मिथुन","कर्क","सिंह","कन्या","तुला","वृश्चिक","धनु","मकर","कुंभ","मीन"],
    "ta": ["மேஷம்","ரிஷபம்","மிதுனம்","கடகம்","சிம்மம்","கன்னி","துலாம்","விருச்சிகம்","தனுசு","மகரம்","கும்பம்","மீனம்"],
    "gu": ["મેષ","વૃષભ","મિથુન","કર્ક","સિંહ","કન્યા","તુલા","વૃશ્ચિક","ધન","મકર","કુંભ","મીન"],
    "kn": ["ಮೇಷ","ವೃಷಭ","ಮಿಥುನ","ಕರ್ಕಾಟಕ","ಸಿಂಹ","ಕನ್ಯಾ","ತುಲಾ","ವೃಶ್ಚಿಕ","ಧನು","ಮಕರ","ಕುಂಭ","ಮೀನ"],
    "ml": ["മേടം","ഇടവം","മിഥുനം","കർക്കടകം","ചിങ്ങം","കന്നി","തുലാം","വൃശ്ചികം","ധനു","മകരം","കുംഭം","മീനം"],
    "pa": ["ਮੇਖ","ਵ੍ਰਿਸ਼ਭ","ਮਿਥੁਨ","ਕਰਕ","ਸਿੰਘ","ਕੰਨਿਆ","ਤੁਲਾ","ਵ੍ਰਿਸ਼ਚਿਕ","ਧਨੁ","ਮਕਰ","ਕੁੰਭ","ਮੀਨ"],
}

def localized_sign(item, language="en"):
    index = next(i for i, value in enumerate(SIGNS) if value[0] == item[0])
    names = SIGN_NAMES.get(language, SIGN_NAMES["en"])
    return {"id": item[0], "name": names[index], "date_range": item[2], "symbol": item[3]}
HOROSCOPES = {"en": {"summary":"Today brings a boost in your career prospects, but be cautious. Emotional tone could lead to introspection, especially related to your social life.","advice":"Focus on self-reflection and communication today. Growth requires patience and understanding."}}
