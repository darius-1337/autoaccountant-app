# AutoAccountant
 
*[Versión en español](README.md)*
 
> **Work in progress.** Some of the features described here aren't finished yet. See *Current status* for what actually works today. This note is temporary.
 
A quarterly tax calculator for self-employed workers in Spain (*autónomos*). It reads expense invoices with an AI model, classifies them against the user's registered business activity, and breaks down how much needs to be set aside for the tax authority.
 
The starting point is simple: billing €30,000 a year doesn't mean taking home €30,000. Between the VAT you collect on behalf of the tax office, the quarterly income tax prepayment and the monthly social security contribution, the gap between what comes in and what's actually yours is wide, and it rarely becomes clear until the quarter closes. This app tries to make that gap visible.
 
<!-- Main screenshot: the results panel with the three headline figures. This is the image that explains the app fastest. -->
![Quarterly results panel](docs/img/panel-resultados.png)
 
## Before you use it
 
**This is not tax advice.** The figures are estimates and don't replace an accountant or a qualified tax adviser. Don't file a *modelo 303* or *modelo 130* based on these numbers without having a professional check them first.
 
Specific risks worth knowing about:
 
- **The AI misreads things.** It can get an amount wrong, apply the wrong VAT rate, or file an expense under the wrong category. Every processed invoice needs reviewing. The app flags the ones it isn't sure about, but it can't catch all of its own mistakes.
- **In cloud mode, your data goes through Google.** The deployed version uses the free tier of the Gemini API, and on that tier Google may use submitted content to improve its models. If you're processing real invoices with sensitive data, run the project locally against your own model.
- **Direct assessment only.** If you're taxed under the *módulos* system (*estimación objetiva*), the calculation works completely differently and this app won't help you.
- **Foreign transactions aren't handled properly yet.** Invoices from non-Spanish suppliers under the reverse charge mechanism (software subscriptions, intra-EU services) are processed, but the tax treatment applied to them is still wrong.
- **Nothing is saved.** Close the tab and everything is gone. That's deliberate, for privacy, but it also means there's no history and no way to pick up where you left off.
## How to use it
 
**1. Pick your business activity.** The search box loads the list of IAE activity codes published by ISTAC. Choose the one you actually registered on your *modelo 036* or *037*, not the one that best describes what you do. They're often not the same thing.
 
**2. Fill in your profile.** It needs your monthly social security contribution (it's on your Seguridad Social receipt) and the year you registered as self-employed. That's enough to work out the income tax provision and whether you still qualify for the reduced withholding rate.
 
**3. Upload your invoices.** Drag the PDFs onto the upload area. They're processed one at a time, and each one's status appears below.
 
<!-- Second screenshot: the processed invoice list, with at least one amber and one red row. Shows the status system at a glance. -->
![Processed invoices in different states](docs/img/facturas-procesadas.png)
 
**4. Check anything flagged in amber.** Some expenses can't be resolved automatically. Vehicle costs, household utilities, travel meals and client entertainment all depend on information a single invoice doesn't contain. Those invoices stay out of the totals until you confirm them. If you uploaded something by mistake, remove it and the totals recalculate on their own.
 
**5. Read the result.** The panel shows total billed, the VAT you need to set aside, and what's genuinely left over.
 
## Things worth knowing
 
**The AI extracts, it doesn't decide.** The model reads the invoice and classifies the expense. The deduction percentages are applied by the backend, using fixed rules written in Java. That split is deliberate: a language model can hallucinate a percentage, and there's real money at stake here. Whenever the AI does suggest a percentage, the invoice gets flagged for manual review.
 
**VAT and income tax are calculated separately.** They don't always match. The clearest case is a mixed-use vehicle: half the VAT is deductible, but nothing is deductible for income tax, because Spanish law doesn't allow partial business use of a vehicle under IRPF.
 
**Your IAE code and your expense rules are two different things.** The code is what you declare, and it determines whether your invoices carry withholding. Deduction rules are applied from a separate activity profile. Tattoo artists, for instance, have to register under codes meant for beauty or personal services — applying a beautician's expense rules to them would be wrong.
 
**There's no database.** Files are held in memory, processed, and discarded. No invoice and no personal data is stored on the server.
 
**The free deployment goes to sleep.** On Render, the service suspends itself after a few minutes of inactivity. The first request after that can take close to a minute. It isn't broken, it's waking up.
 
**Requests are rate-limited per IP** so the free API quota doesn't get burned through. Gemini has its own configurable limits, but I've also added Bucket4j on top. Upload a lot of invoices in quick succession and some will be turned away temporarily.
 
## Current status
 
Expense processing works. Income entry doesn't yet, and until it does the app classifies expenses correctly but can't close out a full quarter, since both the VAT collected and the net profit depend on what's been invoiced.
 
Local mode with a self-hosted model is accounted for in the architecture but not implemented. The AI service sits behind an interface, so adding another provider doesn't mean touching the rest of the codebase.
 
## Configuration
 
Required environment variables:
 
| Variable | Description |
|---|---|
| `GEMINI_API_KEY` | API key from Google AI Studio |
| `GEMINI_API_MODEL` | Model to use. Defaults to the latest Flash |
| `APP_CORS_ALLOWED_ORIGINS` | Allowed origins. No need to touch this locally |
 
## Stack
 
Java with Spring Boot on the backend, plain HTML, CSS and JavaScript on the frontend, served as a single application. No database.
